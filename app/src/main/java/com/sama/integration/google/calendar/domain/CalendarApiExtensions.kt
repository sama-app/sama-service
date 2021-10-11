package com.sama.integration.google.calendar.domain

import com.google.api.services.calendar.Calendar
import com.sama.integration.google.calendar.application.toGoogleCalendarDateTime
import com.sama.integration.sentry.sentrySpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import java.util.UUID
import java.util.concurrent.TimeUnit
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
) = findAllEvents(calendarId, startDateTime, endDateTime, null, singleEvents)

fun Calendar.findAllEvents(calendarId: GoogleCalendarId, syncToken: String, singleEvents: Boolean = true) =
    findAllEvents(calendarId, null, null, syncToken, singleEvents)

private fun Calendar.findAllEvents(
    calendarId: GoogleCalendarId,
    startDateTime: ZonedDateTime?,
    endDateTime: ZonedDateTime?,
    syncToken: String? = null,
    singleEvents: Boolean = true,
): GoogleCalendarEventsResponse {
    sentrySpan(method = "Calendar.findAllEvents") {
        val calendarEvents = mutableListOf<GoogleCalendarEvent>()
        var calendarTimeZone: ZoneId
        var nextPageToken: String? = null
        var nextSyncToken: String?
        do {
            val result =
                findEventsPage(startDateTime, endDateTime, calendarId, nextPageToken, syncToken, singleEvents).execute()
            calendarTimeZone = result.timeZone?.let { ZoneId.of(it) } ?: UTC
            nextPageToken = result.nextPageToken
            nextSyncToken = result.nextSyncToken

            calendarEvents.addAll(result.items)
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
    nextPageToken?.run { requestBuilder.setPageToken(this) }
    return requestBuilder
}

fun Calendar.createCalendarsChannel(channelId: UUID, inputToken: String, callbackUrl: String): Calendar.CalendarList.Watch {
    return calendarList().watch(GoogleChannel().apply {
        id = channelId.toString()
        type = "webhook"
        address = callbackUrl
        token = inputToken
        expiration = Instant.now().plus(1, MONTHS).toEpochMilli()
    })
}

fun Calendar.createEventsChannel(
    calendarId: GoogleCalendarId,
    channelId: UUID,
    inputToken: String,
    callbackUrl: String
): Calendar.Events.Watch {
    return events().watch(calendarId, GoogleChannel().apply {
        id = channelId.toString()
        type = "webhook"
        address = callbackUrl
        token = inputToken
        expiration = Instant.now().plus(1, MONTHS).toEpochMilli()
    })
}

fun Calendar.stopChannel(channelId: UUID, resourceId: String) {
    channels().stop(GoogleChannel().apply {
        this.id = channelId.toString()
        this.resourceId = resourceId
    })
}