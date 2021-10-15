package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.EventDateTime
import com.sama.integration.sentry.sentrySpan
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private var logger: Logger = LoggerFactory.getLogger(Calendar::class.java)

fun Calendar.insert(calendarId: String, googleCalendarEvent: GoogleCalendarEvent): GoogleCalendarEvent {
    sentrySpan(method = "Calendar.insert") {
        return events()
            .insert(calendarId, googleCalendarEvent)
            .setSendNotifications(true)
            .setSendUpdates("all")
            .setConferenceDataVersion(1)
            .execute()
    }
}

fun Calendar.findAllEvents(
    calendarId: GoogleCalendarId,
    startDate: LocalDate,
    endDate: LocalDate,
    singleEvents: Boolean = true,
) = findAllEvents(calendarId, startDate.atStartOfDay(UTC), endDate.atStartOfDay(UTC), null, singleEvents)

fun Calendar.findAllEvents(
    calendarId: GoogleCalendarId,
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime,
    singleEvents: Boolean = true,
    createdFrom: ZonedDateTime? = null,
    minAttendeeCount: Int? = null
) = findAllEvents(calendarId, startDateTime, endDateTime, null, singleEvents, createdFrom, minAttendeeCount)

fun Calendar.findAllEvents(calendarId: GoogleCalendarId, syncToken: String, singleEvents: Boolean = true) =
    findAllEvents(calendarId, null, null, syncToken, singleEvents)

private fun Calendar.findAllEvents(
    calendarId: GoogleCalendarId,
    startDateTime: ZonedDateTime?,
    endDateTime: ZonedDateTime?,
    syncToken: String? = null,
    singleEvents: Boolean = true,
    createdFrom: ZonedDateTime? = null,
    minAttendeeCount: Int? = null
): GoogleCalendarEventsResponse {
    sentrySpan(method = "Calendar.findAllEvents") {
        val calendarEvents = mutableListOf<GoogleCalendarEvent>()
        var calendarTimeZone: ZoneId
        var nextPageToken: String? = null
        var nextSyncToken: String?
        do {
            val result = findEventsPage(
                startDateTime, endDateTime, calendarId, nextPageToken, syncToken, singleEvents
            ).execute()
            calendarTimeZone = result.timeZone?.let { ZoneId.of(it) } ?: UTC
            nextPageToken = result.nextPageToken
            nextSyncToken = result.nextSyncToken

            var items = result.items
            if (createdFrom != null || minAttendeeCount != null) {
                items = items.filter {
                    (createdFrom == null || it.created == null || !it.created.toZonedDateTime().isBefore(createdFrom))
                            && (minAttendeeCount == null || it.attendeeCount() >= minAttendeeCount)
                }
            }

            calendarEvents.addAll(items)
        } while (nextPageToken != null)

        return GoogleCalendarEventsResponse(calendarEvents, calendarTimeZone, nextSyncToken)
    }
}

private fun Calendar.findEventsPage(
    startDateTime: ZonedDateTime?,
    endDateTime: ZonedDateTime?,
    calendarId: GoogleCalendarId,
    nextPageToken: String?,
    syncToken: String? = null,
    singleEvents: Boolean,
): Calendar.Events.List {
    val requestBuilder = this.events().list(calendarId)
    requestBuilder.singleEvents = singleEvents
    requestBuilder.maxResults = 2500

    if (syncToken != null) {
        requestBuilder.syncToken = syncToken
    } else {
        requestBuilder.timeMin = startDateTime?.toGoogleCalendarDateTime()
        requestBuilder.timeMax = endDateTime?.toGoogleCalendarDateTime()
    }
    requestBuilder.fields =
        "nextPageToken,nextSyncToken,timeZone,items(id,status,start,end,summary,attendees,recurringEventId,created)"

    nextPageToken?.run { requestBuilder.setPageToken(this) }
    return requestBuilder
}

fun Calendar.findRecurrenceRules(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime,
    calendarId: GoogleCalendarId,
): Map<String, RecurrenceRule?> {
    sentrySpan(method = "Calendar.findRecurrenceRules") {
        val recurringEvents = findAllEvents(calendarId, startDateTime, endDateTime, null, false).events
            .filter { it.recurrence != null }

        return recurringEvents
            .associate { event ->
                try {
                    val recurrenceRule = event.recurrence
                        .filter { "RRULE:" in it }
                        .map { RecurrenceRule(it.replace("RRULE:", "")) }
                        .first()
                    event.id to recurrenceRule
                } catch (e: Exception) {
                    logger.error("Could not process RRULE: %s".format(event.recurrence), e)
                    event.id to null
                }
            }
    }
}

fun Calendar.findAllCalendars(syncToken: String?): GoogleCalendarListResponse {
    sentrySpan(method = "Calendar.findAllCalendars") {
        val calendars = mutableListOf<GoogleCalendar>()
        var nextPageToken: String? = null
        var nextSyncToken: String?
        do {
            val result = findCalendarsPage(nextPageToken, syncToken).execute()
            nextPageToken = result.nextPageToken
            nextSyncToken = result.nextSyncToken

            calendars.addAll(result.items)
        } while (nextPageToken != null)

        return GoogleCalendarListResponse(calendars, nextSyncToken)
    }
}

private fun Calendar.findCalendarsPage(nextPageToken: String?, syncToken: String? = null): Calendar.CalendarList.List {
    val requestBuilder = this.calendarList().list()
    requestBuilder.maxResults = 250
    requestBuilder.syncToken = syncToken
    requestBuilder.fields = "nextPageToken,nextSyncToken,items(id,timeZone,accessRole,primary,selected)"
    nextPageToken?.run { requestBuilder.setPageToken(this) }
    return requestBuilder
}

fun Calendar.createCalendarsChannel(
    channelId: UUID, inputToken: String, callbackUrl: String, expiresAt: Instant
): Calendar.CalendarList.Watch =
    calendarList().watch(GoogleChannel().apply {
        id = channelId.toString()
        type = "webhook"
        address = callbackUrl
        token = inputToken
        expiration = expiresAt.toEpochMilli()
    })


fun Calendar.createEventsChannel(
    calendarId: GoogleCalendarId, channelId: UUID, inputToken: String, callbackUrl: String, expiresAt: Instant
): Calendar.Events.Watch =
    events().watch(calendarId, GoogleChannel().apply {
        id = channelId.toString()
        type = "webhook"
        address = callbackUrl
        token = inputToken
        expiration = expiresAt.toEpochMilli()
    })

fun Calendar.stopChannel(channelId: UUID, resourceId: String): Calendar.Channels.Stop =
    channels().stop(GoogleChannel().apply {
        this.id = channelId.toString()
        this.resourceId = resourceId
    })


fun ZonedDateTime.toGoogleCalendarDateTime() =
    GoogleCalendarDateTime(Date.from(this.toInstant()), TimeZone.getTimeZone(this.zone))

fun EventDateTime.toZonedDateTime(defaultZoneId: ZoneId): ZonedDateTime {
    if (this.date != null) {
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.date.value), UTC)
        return ZonedDateTime.of(localDateTime, defaultZoneId)
    }
    if (this.dateTime != null) {
        return dateTime.toZonedDateTime()

    }
    throw IllegalArgumentException("invalid EventDateTime")
}

fun DateTime.toZonedDateTime(): ZonedDateTime = ZonedDateTime.parse(toStringRfc3339())
