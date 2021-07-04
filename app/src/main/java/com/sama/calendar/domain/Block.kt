package com.sama.calendar.domain

import java.time.ZonedDateTime
import java.util.*

/**
 * Represents a blocked part of a calendar
 */
data class Block(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?,
    val recipientEmail: String?
) {
    fun multiDay() = !startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate())
}