package com.sama.calendar.domain

import java.time.ZonedDateTime

/**
 * Represents a blocked part of a calendar
 */
data class Block(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?,
    val description: String?,
    val recipientEmail: String?,
    val recurrenceCount: Int,
    val recurrenceRule: RecurrenceRule?
) {
    fun multiDay() = !startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate())
}

data class RecurrenceRule(
    val recurrence: Recurrence,
    val interval: Int
)

enum class Recurrence {
    YEARLY,
    MONTHLY,
    WEEKLY,
    DAILY,
}