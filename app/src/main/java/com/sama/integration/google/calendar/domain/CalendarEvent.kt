package com.sama.integration.google.calendar.domain

import com.sama.users.domain.UserId
import java.time.ZonedDateTime

data class GoogleCalendarEventKey(
    val userId: UserId,
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
