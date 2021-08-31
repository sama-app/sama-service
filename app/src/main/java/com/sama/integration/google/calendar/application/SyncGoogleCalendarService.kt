package com.sama.integration.google.calendar.application

import com.google.api.services.calendar.model.ConferenceData
import com.google.api.services.calendar.model.ConferenceSolutionKey
import com.google.api.services.calendar.model.CreateConferenceRequest
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.common.ApplicationService
import com.sama.common.component1
import com.sama.common.component2
import com.sama.common.to
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.GoogleSyncTokenInvalidatedException
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarSync
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.calendar.domain.insert
import com.sama.integration.google.translatedGoogleException
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@ApplicationService
class SyncGoogleCalendarService(
    private val googleServiceFactory: GoogleServiceFactory,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val clock: Clock,
) : GoogleCalendarService {
    private var logger: Logger = LoggerFactory.getLogger(SyncGoogleCalendarService::class.java)


    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findEvents(
        userId: UserId,
        calendarId: GoogleCalendarId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
    ): List<CalendarEvent> {
        // Pull from local event cache if it's up-to-date
        val calendarSync = calendarSyncRepository.find(userId, calendarId)
        val isSynced = calendarSync?.isSyncedFor(startDateTime, endDateTime, clock) ?: false
        val calendarEvents = if (isSynced) {
            calendarEventRepository.findAll(userId, calendarId, startDateTime, endDateTime)
        }
        // Fallback to making an API request to GCal API
        else {
            try {
                val calendarService = googleServiceFactory.calendarService(userId)
                val (events, timeZone) = calendarService.findAllEvents(calendarId, startDateTime, endDateTime)
                events.toDomain(userId, calendarId, timeZone)
            } catch (e: Exception) {
                throw translatedGoogleException(e)
            }
        }

        // Compute aggregate values
        val recurringEventCounts = calendarEvents
            .filter { it.eventData.recurringEventId != null }
            .groupingBy { it.eventData.recurringEventId }
            .eachCount()

        val eventsWithAggregateDate = calendarEvents.map { event ->
            val recurrenceCount = event.eventData.recurringEventId?.let { recurringEventCounts[it] } ?: 0
            event.copy(aggregatedData = AggregatedData(recurrenceCount))
        }

        return eventsWithAggregateDate
    }

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun insertEvent(
        userId: UserId,
        calendarId: GoogleCalendarId,
        command: InsertGoogleCalendarEventCommand,
    ): CalendarEvent {
        return try {
            val timeZone = command.startDateTime.zone
            val googleCalendarEvent = GoogleCalendarEvent().apply {
                start = EventDateTime()
                    .setDateTime(command.startDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                end = EventDateTime()
                    .setDateTime(command.endDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                attendees = listOf(
                    EventAttendee().apply {
                        email = command.recipientEmail
                    },
                )
                summary = command.title
                description = command.description
                conferenceData = ConferenceData().apply {
                    createRequest = CreateConferenceRequest().apply {
                        requestId = UUID.randomUUID().toString()
                        conferenceSolutionKey = ConferenceSolutionKey().apply {
                            type = "hangoutsMeet"
                        }

                    }
                }
            }

            val calendarService = googleServiceFactory.calendarService(userId)
            val inserted = calendarService.insert(calendarId, googleCalendarEvent)

            syncUserCalendar(userId, calendarId)

            inserted.toDomain(userId, calendarId, timeZone)
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }

    @Transactional
    override fun enableCalendarSync(userId: UserId, calendarId: GoogleCalendarId) {
        val calendarSync = calendarSyncRepository.find(userId, calendarId)
            ?.forceSync(clock)
            ?: CalendarSync.new(userId, calendarId, clock)
        calendarSyncRepository.save(calendarSync)
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 15000)
    fun syncUserCalendars() {
        val userCalendarsToSync = calendarSyncRepository.findSyncable(Instant.now())
        userCalendarsToSync.forEach {
            syncUserCalendar(it.first, it.second)
        }
    }

    @Transactional
    private fun syncUserCalendar(userId: UserId, calendarId: GoogleCalendarId, forceFullSync: Boolean = false) {
        val calendarSync = calendarSyncRepository.findAndLock(userId, calendarId)
            ?: CalendarSync.new(userId, calendarId, clock)

        logger.info("Syncing User#${userId.id} Calendar#${calendarId}...")
        val calendarService = googleServiceFactory.calendarService(userId)
        try {
            val updatedSync = if (forceFullSync || calendarSync.needsFullSync(clock)) {
                val (startDate, endDate) = CalendarSync.syncRange(clock)
                val (events, timeZone, syncToken) = calendarService
                    .findAllEvents(calendarId, startDate, endDate)

                val calendarEvents = events.toDomain(userId, calendarId, timeZone)
                calendarEventRepository.deleteBy(userId, calendarId)
                calendarEventRepository.saveAll(calendarEvents)

                calendarSync.completeFull(syncToken!!, startDate to endDate, clock)
            } else {
                val (events, timeZone, newSyncToken) = calendarService
                    .findAllEvents(calendarId, calendarSync.syncToken!!)

                val (toAdd, toRemove) = events.partition { it.status in ACCEPTED_EVENT_STATUSES }
                calendarEventRepository.saveAll(toAdd.toDomain(userId, calendarId, timeZone))
                calendarEventRepository.deleteAll(toRemove.map { it.toKey(userId, calendarId) })

                calendarSync.complete(newSyncToken!!, clock)
            }

            calendarSyncRepository.save(updatedSync)
            logger.info("Completed sync for User#${userId.id} Calendar#${calendarId}...")
        } catch (e: Exception) {
            if (e is GoogleSyncTokenInvalidatedException) {
                logger.error("Calendar sync token expired for User#${userId.id}", e)
                val updated = calendarSync.reset(clock)
                calendarEventRepository.deleteBy(userId, calendarId)
                calendarSyncRepository.save(updated)
            } else {
                val updated = calendarSync.fail(clock)
                logger.error("Failed to sync Calendar for User#${userId.id} ${updated.failedSyncCount} times", e)
                calendarSyncRepository.save(updated)
            }
        }
    }
}