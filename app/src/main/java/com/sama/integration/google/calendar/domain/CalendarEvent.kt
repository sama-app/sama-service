package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZoneId
import java.time.ZonedDateTime

data class GoogleCalendarEventKey(
    val accountId: GoogleAccountId,
    val calendarId: GoogleCalendarId,
    val eventId: GoogleCalendarEventId,
)

data class CalendarEvent(
    val key: GoogleCalendarEventKey,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val eventData: EventData,
    val aggregatedData: AggregatedData? = null,
)

data class EventData(
    val title: String? = null,
    val allDay: Boolean,
    val attendeeCount: Int,
    val recurringEventId: GoogleCalendarEventId? = null,
    val created: ZonedDateTime? = null
)

data class AggregatedData(
    val recurrenceCount: Int,
)


typealias GoogleCalendarEvent = Event
typealias GoogleCalendarEventId = String
typealias GoogleCalendarDateTime = DateTime

data class GoogleCalendarEventsResponse(
    val events: List<GoogleCalendarEvent>, val timeZone: ZoneId, val syncToken: String?,
)

val ACCEPTED_EVENT_STATUSES = listOf("confirmed", "tentative")

fun Collection<GoogleCalendarEvent>.toDomain(accountId: GoogleAccountId, calendarId: GoogleCalendarId, timeZone: ZoneId) =
    filter { it.status in ACCEPTED_EVENT_STATUSES }
        .map { it.toDomain(accountId, calendarId, timeZone) }


fun GoogleCalendarEvent.toDomain(accountId: GoogleAccountId, calendarId: GoogleCalendarId, timeZone: ZoneId): CalendarEvent {
    return CalendarEvent(
        toKey(accountId, calendarId),
        start.toZonedDateTime(timeZone),
        end.toZonedDateTime(timeZone),
        EventData(summary, isAllDay(), attendeeCount(), recurringEventId, created?.toZonedDateTime())
    )
}

fun GoogleCalendarEvent.toKey(accountId: GoogleAccountId, calendarId: GoogleCalendarId): GoogleCalendarEventKey {
    return GoogleCalendarEventKey(accountId, calendarId, id)
}

fun GoogleCalendarEvent.isAllDay(): Boolean {
    return start.date != null
}

fun GoogleCalendarEvent.attendeeCount(): Int {
    return attendees?.size ?: 0
}

fun Collection<GoogleCalendarEvent>.attendeeEmails(): Set<String> {
    return asSequence() // more efficient
        .mapNotNull { it.attendees }
        .flatten()
        .distinct()
        .map { it.email }
        .toSet()
}
