package com.sama.integration.google

import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.sama.integration.sentry.sentrySpan
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime

private var logger: Logger = LoggerFactory.getLogger(Calendar::class.java)
private val defaultCalendarTimeZone = ZoneId.of("UTC")

fun Calendar.insert(googleCalendarEvent: Event): Event {
    sentrySpan(method = "Calendar.insert") {
        val inserted = events()
            .insert("primary", googleCalendarEvent)
            .setSendNotifications(true)
            .setConferenceDataVersion(1)
            .execute()
        return inserted
    }
}

fun Calendar.findAllEvents(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime,
    singleEvents: Boolean = true
): Pair<MutableList<GoogleCalendarEvent>, ZoneId> {
    sentrySpan(method = "Calendar.findAllEvents") {
        val calendarEvents = mutableListOf<GoogleCalendarEvent>()
        var calendarTimeZone: ZoneId
        var nextPageToken: String? = null
        do {
            val result = this.findEventsPage(startDateTime, endDateTime, nextPageToken, singleEvents).execute()
            calendarTimeZone = result.timeZone?.let { ZoneId.of(it) } ?: defaultCalendarTimeZone
            nextPageToken = result.nextPageToken

            calendarEvents.addAll(result.items
                .filter { it.status in listOf("confirmed", "tentative") })
        } while (nextPageToken != null)

        return Pair(calendarEvents, calendarTimeZone)
    }
}

private fun Calendar.findEventsPage(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime,
    nextPageToken: String?,
    singleEvents: Boolean
): Calendar.Events.List {
    var requestBuilder = this.events().list("primary")
        .setMaxResults(2500)
        .setTimeMin(startDateTime.toGoogleCalendarDateTime())
        .setTimeMax(endDateTime.toGoogleCalendarDateTime())

    if (singleEvents) {
        requestBuilder = requestBuilder.setOrderBy("startTime")
            .setSingleEvents(true)
    } else {
        requestBuilder.singleEvents = false
    }
    nextPageToken?.run { requestBuilder.setPageToken(this) }
    return requestBuilder
}

fun Calendar.findRecurrenceRules(
    startDateTime: ZonedDateTime,
    endDateTime: ZonedDateTime
): Map<String, RecurrenceRule?> {
    sentrySpan(method = "Calendar.findRecurringEvents") {
        val recurringEvents = findAllEvents(startDateTime, endDateTime, false).first
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
