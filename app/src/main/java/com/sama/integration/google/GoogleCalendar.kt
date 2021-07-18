package com.sama.integration.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.EventDateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

private var logger: Logger = LoggerFactory.getLogger(Calendar::class.java)
private val defaultCalendarTimeZone = ZoneId.of("UTC")


fun Calendar.list(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime,
    nextPageToken: String?
): Calendar.Events.List {
    val requestBuilder = this.events().list("primary")
        .setMaxResults(2500)
        .setTimeMin(startDateTime.toGoogleCalendarDateTime())
        .setTimeMax(endDateTime.toGoogleCalendarDateTime())
        .setOrderBy("startTime")
        .setSingleEvents(true)
    nextPageToken?.run { requestBuilder.setPageToken(this) }
    return requestBuilder
}


fun Calendar.listAll(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime
): Pair<MutableList<GoogleCalendarEvent>, ZoneId> {
    val calendarEvents = mutableListOf<GoogleCalendarEvent>()
    var calendarTimeZone: ZoneId
    var nextPageToken: String? = null
    do {
        val result = this.list(startDateTime, endDateTime, nextPageToken).execute()
        calendarTimeZone = result.timeZone?.let { ZoneId.of(it) } ?: defaultCalendarTimeZone
        nextPageToken = result.nextPageToken

        calendarEvents.addAll(result.items
            .filter { it.status in listOf("confirmed", "tentative") })
    } while (nextPageToken != null)

    return Pair(calendarEvents, calendarTimeZone)
}

fun Calendar.recurrenceEventsFor(calendarEvents: List<GoogleCalendarEvent>): Map<String, RecurrenceRule?> {
    val recurringEventIDs = calendarEvents
        .filter { it.recurringEventId != null }
        .map { it.recurringEventId }
        .toSet()
    return recurringEventIDs
        .mapNotNull {
            try {
                this.events().get("primary", it).execute()
            } catch (e: GoogleJsonResponseException) {
                // The original recurring event might have been deleted but the single
                // event still refers to it; ignore such cases.
                if (e.statusCode != 404) {
                    logger.error("Could not fetch Google Calendar Event: %s".format(it), e)
                }
                null
            }
        }
        .filter { event -> event.recurrence.any { "RRULE:" in it } }
        .associate { event ->
            val eventId = event.id
            event
                .runCatching {
                    val recurrenceRule = event.recurrence
                        .filter { "RRULE:" in it }
                        .map { RecurrenceRule(it.replace("RRULE:", "")) }
                        .first()
                    eventId to recurrenceRule
                }
                .onFailure { logger.error("Could not process RRULE: %s".format(event.recurrence), it) }
                .getOrDefault(eventId to null)
        }
}
