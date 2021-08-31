package com.sama.integration.google.calendar.application

import com.google.api.services.calendar.model.EventDateTime
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarDateTime
import com.sama.integration.google.calendar.domain.GoogleCalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarEventKey
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.attendeeCount
import com.sama.integration.google.calendar.domain.isAllDay
import com.sama.users.domain.UserId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import java.util.TimeZone


fun ZonedDateTime.toGoogleCalendarDateTime() =
    GoogleCalendarDateTime(Date.from(this.toInstant()), TimeZone.getTimeZone(this.zone))

fun EventDateTime.toZonedDateTime(defaultZoneId: ZoneId): ZonedDateTime {
    if (this.date != null) {
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.date.value), ZoneOffset.UTC)
        return ZonedDateTime.of(localDateTime, defaultZoneId)
    }
    if (this.dateTime != null) {
        return ZonedDateTime.parse(this.dateTime.toStringRfc3339())

    }
    throw IllegalArgumentException("invalid EventDateTime")
}

val ACCEPTED_EVENT_STATUSES = listOf("confirmed", "tentative")

fun Collection<GoogleCalendarEvent>.toDomain(
    userId: UserId, calendarId: GoogleCalendarId, timeZone: ZoneId,
): List<CalendarEvent> {
    return filter { it.status in ACCEPTED_EVENT_STATUSES }
        .map { it.toDomain(userId, calendarId, timeZone) }
}

fun GoogleCalendarEvent.toKey(userId: UserId, calendarId: GoogleCalendarId): GoogleCalendarEventKey {
    return GoogleCalendarEventKey(userId, calendarId, id)
}

fun GoogleCalendarEvent.toDomain(userId: UserId, calendarId: GoogleCalendarId, timeZone: ZoneId): CalendarEvent {
    return CalendarEvent(
        toKey(userId, calendarId),
        start.toZonedDateTime(timeZone),
        end.toZonedDateTime(timeZone),
        EventData(summary, isAllDay(), attendeeCount(), recurringEventId)
    )
}