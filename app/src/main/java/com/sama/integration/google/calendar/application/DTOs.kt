package com.sama.integration.google.calendar.application

import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import java.time.ZonedDateTime

data class CalendarEventDTO(
    val accountId: GoogleAccountPublicId,
    val calendarId: GoogleCalendarId,
    val eventId: GoogleCalendarEventId,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val eventData: EventData,
    val aggregatedData: AggregatedData,
)

data class CalendarsDTO(
    val calendars: List<CalendarDTO>
)

data class CalendarDTO(
    val accountId: GoogleAccountPublicId,
    val calendarId: GoogleCalendarId,
    val selected: Boolean,
    val title: String?,
    val colour: String?,
)