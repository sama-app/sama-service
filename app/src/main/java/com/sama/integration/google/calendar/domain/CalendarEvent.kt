package com.sama.integration.google.calendar.domain

import com.sama.users.domain.UserId
import java.time.ZonedDateTime


data class CalendarEvent(
    val userId: UserId,
    val calendarId: GoogleCalendarId,
    val googleEventId: GoogleCalendarEventId,
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
