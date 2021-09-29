package com.sama.integration.google.calendar.domain

import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.users.domain.UserId
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
    var title: String? = null,
    val allDay: Boolean,
    val attendeeCount: Int,
    var recurringEventId: GoogleCalendarEventId? = null,
)

data class AggregatedData(
    val recurrenceCount: Int,
)
