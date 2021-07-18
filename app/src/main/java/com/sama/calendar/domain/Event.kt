package com.sama.calendar.domain

import java.time.ZonedDateTime

/**
 * Represents a blocked part of a calendar
 */
data class Event(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?,
    val description: String?,
    val recipientEmail: String?,
)