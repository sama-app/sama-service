package com.sama.calendar.application

import com.sama.common.ZonedDateTimeRange
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import java.time.ZonedDateTime

data class EventDTO(
    override val startDateTime: ZonedDateTime,
    override val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?,
    val accountId: GoogleAccountPublicId,
    val calendarId: String,
    val eventId: GoogleCalendarEventId,
    val meetingBlock: Boolean
): ZonedDateTimeRange

data class EventsDTO(
    val events: List<EventDTO>
)