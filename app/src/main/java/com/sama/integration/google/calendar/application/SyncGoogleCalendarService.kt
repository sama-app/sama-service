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
import com.sama.connection.application.AddDiscoveredUsersCommand
import com.sama.connection.application.UserConnectionService
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.GoogleSyncTokenInvalidatedException
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.integration.google.calendar.domain.CalendarSync
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.calendar.domain.insert
import com.sama.integration.google.translatedGoogleException
import com.sama.integration.sentry.sentrySpan
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPermission
import com.sama.users.domain.UserPermission.PAST_EVENT_CONTACT_SCAN
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.dao.CannotAcquireLockException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ApplicationService
class SyncGoogleCalendarService(
    private val googleServiceFactory: GoogleServiceFactory,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarListSyncRepository: CalendarListSyncRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer,
    private val userSettingsService: UserSettingsService,
    private val userConnectionService: UserConnectionService,
    private val clock: Clock,
) : GoogleCalendarService {

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findEvents(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
    ): List<CalendarEvent> {
        val calendarSyncs = calendarSyncRepository.findAll(userId)

        val calendarEvents = if (calendarSyncs.isNotEmpty()) {
            calendarSyncs
                .map { sync ->
                    val isSynced = sync.isSyncedFor(startDateTime, endDateTime, clock)
                    if (isSynced) {
                        calendarEventRepository.findAll(userId, sync.calendarId, startDateTime, endDateTime)
                    } else {
                        forceLoadCalendarEvents(userId, sync.calendarId, startDateTime, endDateTime)
                    }
                }
                .flatten()
        } else {
            // If there aren't any synced calendars, load the primary one
            forceLoadCalendarEvents(userId, PRIMARY_CALENDAR_ID, startDateTime, endDateTime)
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

    private fun forceLoadCalendarEvents(
        userId: UserId,
        calendarId: GoogleCalendarId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ) = try {
        val calendarService = googleServiceFactory.calendarService(userId)
        val (events, timeZone) = calendarService.findAllEvents(calendarId, startDateTime, endDateTime)
        events.toDomain(userId, PRIMARY_CALENDAR_ID, timeZone)
    } catch (e: Exception) {
        throw translatedGoogleException(e)
    }

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun insertEvent(
        userId: UserId,
        calendarId: GoogleCalendarId,
        command: InsertGoogleCalendarEventCommand
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
                        email = command.initiatorEmail
                        responseStatus = "accepted"
                    },
                    EventAttendee().apply {
                        email = command.recipientEmail
                        responseStatus = "accepted"
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

            googleCalendarSyncer.syncUserCalendar(userId, calendarId)

            inserted.toDomain(userId, calendarId, timeZone)
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }

    override fun enableCalendarSync(userId: UserId) {
        googleCalendarSyncer.enableCalendarListSync(userId)
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 25000)
    fun syncUserCalendarLists() {
        sentrySpan(method = "syncUserCalendarLists") {
            val userCalendarListsToSync = calendarListSyncRepository.findSyncable(Instant.now())
            userCalendarListsToSync.forEach { userId ->
                googleCalendarSyncer.syncUserCalendarList(userId)
            }
        }
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 25000)
    fun syncUserCalendars() {
        sentrySpan(method = "syncUserCalendars") {
            val userCalendarsToSync = calendarSyncRepository.findSyncable(Instant.now())
            userCalendarsToSync.forEach { (userId, calendarId) ->
                googleCalendarSyncer.syncUserCalendar(userId, calendarId)
            }
        }
    }
}