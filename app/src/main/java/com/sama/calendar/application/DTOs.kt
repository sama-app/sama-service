package com.sama.calendar.application

import java.time.ZonedDateTime

data class EventDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?
)

data class FetchEventsDTO(
    val events: List<EventDTO>
)