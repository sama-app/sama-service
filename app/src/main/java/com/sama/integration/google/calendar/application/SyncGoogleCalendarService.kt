package com.sama.integration.google.calendar.application

import com.google.api.services.calendar.model.ConferenceData
import com.google.api.services.calendar.model.ConferenceSolutionKey
import com.google.api.services.calendar.model.CreateConferenceRequest
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.NoPrimaryGoogleAccountException
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.PRIMARY_CALENDAR_ID
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.calendar.domain.insert
import com.sama.integration.google.calendar.domain.toDomain
import com.sama.integration.google.calendar.domain.toGoogleCalendarDateTime
import com.sama.integration.google.translatedGoogleException
import com.sama.integration.sentry.sentrySpan
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ApplicationService
class SyncGoogleCalendarService(
    private val googleServiceFactory: GoogleServiceFactory,
    private val googleAccountRepository: GoogleAccountRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarListSyncRepository: CalendarListSyncRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer,
    private val clock: Clock,
) : GoogleCalendarService {

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findEvents(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime?
    ): List<CalendarEvent> {
        val accountIds = googleAccountRepository.findAllByUserId(userId)
            .filter { it.linked }
            .map { it.id!! }
            .toSet()
        val calendarSyncs = calendarSyncRepository.findAll(accountIds)

        val calendarEvents = if (calendarSyncs.isNotEmpty()) {
            calendarSyncs
                .map { sync ->
                    val isSynced = sync.isSyncedFor(startDateTime, endDateTime, clock)
                    if (isSynced) {
                        calendarEventRepository.findAll(sync.accountId, sync.calendarId, startDateTime, endDateTime, createdFrom)
                    } else {
                        forceLoadCalendarEvents(sync.accountId, sync.calendarId, startDateTime, endDateTime, createdFrom)
                    }
                }
                .flatten()
        } else {
            // If there aren't any synced calendars, load the primary calendar for all accounts
            accountIds
                .map { forceLoadCalendarEvents(it, PRIMARY_CALENDAR_ID, startDateTime, endDateTime, createdFrom) }
                .flatten()
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
        accountId: GoogleAccountId,
        calendarId: GoogleCalendarId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime?
    ) = try {
        val calendarService = googleServiceFactory.calendarService(accountId)
        val (events, timeZone) = calendarService.findAllEvents(calendarId, startDateTime, endDateTime, createdFrom = createdFrom)
        events.toDomain(accountId, PRIMARY_CALENDAR_ID, timeZone)
    } catch (e: Exception) {
        throw translatedGoogleException(e)
    }

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): CalendarEvent {
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

            val primaryAccountId = googleAccountRepository.findByUserIdAndPrimary(userId)
                ?: throw NoPrimaryGoogleAccountException(userId)
            val calendarId = PRIMARY_CALENDAR_ID

            val calendarService = googleServiceFactory.calendarService(primaryAccountId)
            val inserted = calendarService.insert(calendarId, googleCalendarEvent)

            googleCalendarSyncer.syncUserCalendar(primaryAccountId, calendarId)

            inserted.toDomain(primaryAccountId, calendarId, timeZone)
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
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
            userCalendarsToSync.forEach { (accountId, calendarId) ->
                googleCalendarSyncer.syncUserCalendar(accountId, calendarId)
            }
        }
    }
}