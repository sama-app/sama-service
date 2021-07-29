package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.SamaApplication
import com.sama.calendar.domain.Event
import com.sama.calendar.domain.EventRepository
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.BlockRepository
import com.sama.slotsuggestion.domain.Recurrence
import com.sama.users.domain.UserId
import liquibase.pro.packaged.it
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class GoogleCalendarEventRepository(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleNetHttpTransport: HttpTransport,
    private val googleJacksonFactory: JacksonFactory
) : EventRepository, BlockRepository {

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun save(userId: UserId, event: Event): Event {
        return kotlin.runCatching {
            val calendarService = calendarServiceForUser(userId)
            val timeZone = event.startDateTime.zone
            val googleCalendarEvent = GoogleCalendarEvent().apply {
                start = EventDateTime()
                    .setDateTime(event.startDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                end = EventDateTime()
                    .setDateTime(event.endDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                attendees = listOf(
                    EventAttendee().apply {
                        email = event.recipientEmail
                    },
                )
                summary = event.title
                description = event.description
            }

            val inserted = calendarService.events()
                .insert("primary", googleCalendarEvent)
                .setSendNotifications(true)
                .execute()
            inserted.toEvent(timeZone)
        }
            .onFailure(handleGoogleExceptions(userId))
            .getOrThrow()
    }

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findAll(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): List<Event> {
        return kotlin.runCatching {
            val calendarService = calendarServiceForUser(userId)
            val (calendarEvents, calendarTimeZone) = calendarService.listAll(startDateTime, endDateTime)
            calendarEvents.map { it.toEvent(calendarTimeZone) }
        }
            .onFailure(handleGoogleExceptions(userId))
            .getOrThrow()
    }

    private fun GoogleCalendarEvent.toEvent(calendarTimeZone: ZoneId): Event {
        return Event(
            this.start.toZonedDateTime(calendarTimeZone),
            this.end.toZonedDateTime(calendarTimeZone),
            this.isAllDay(),
            this.summary,
            this.description,
            if (this.attendees != null) {
                this.attendees.firstOrNull()?.email
            } else {
                null
            }
        )
    }

    override fun findAllBlocks(
        userId: Long,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ): Collection<Block> {
        return kotlin.runCatching {
            val calendarService = calendarServiceForUser(userId)

            val (calendarEvents, calendarTimeZone) = calendarService.listAll(startDateTime, endDateTime)
            val recurrenceRulesByEventID = calendarService.recurrenceEventsFor(calendarEvents)
            val recurringEventCounts = calendarEvents.filter { it.recurringEventId != null }
                .groupingBy { it.recurringEventId }
                .eachCount()

            calendarEvents
                .map { it.toBlock(calendarTimeZone, recurringEventCounts, recurrenceRulesByEventID) }
        }
            .onFailure(handleGoogleExceptions(userId))
            .getOrThrow()
    }

    private fun GoogleCalendarEvent.toBlock(
        calendarTimeZone: ZoneId,
        recurrenceCounts: Map<String, Int>,
        recurringEvents: Map<String, RecurrenceRule?>
    ): Block {
        return Block(
            this.start.toZonedDateTime(calendarTimeZone),
            this.end.toZonedDateTime(calendarTimeZone),
            this.isAllDay(),
            this.attendees != null && this.attendees.size > 0,

            if (this.recurringEventId != null && recurringEventId in recurrenceCounts) {
                recurrenceCounts[recurringEventId]!!
            } else {
                1
            },

            if (this.recurringEventId != null && recurringEventId in recurringEvents) {
                recurringEvents[recurringEventId]
                    ?.takeIf { it.freq in listOf(Freq.DAILY, Freq.WEEKLY, Freq.MONTHLY, Freq.YEARLY) }
                    ?.let {
                        com.sama.slotsuggestion.domain.RecurrenceRule(
                            Recurrence.valueOf(it.freq.name),
                            it.interval
                        )
                    }
            } else {
                null
            }
        )
    }

    private fun calendarServiceForUser(userId: Long): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Calendar.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    private fun handleGoogleExceptions(userId: UserId): (exception: Throwable) -> Unit = {
        if (it is GoogleJsonResponseException) {
            when (it.statusCode) {
                401 -> throw GoogleInvalidCredentialsException(userId, it)
                403 -> throw GoogleInsufficientPermissionsException(userId, it)
                else -> throw GoogleUnhandledException(it)
            }
        }
        throw it
    }
}