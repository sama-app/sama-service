package com.sama.slotsuggestion.domain

import com.sama.calendar.domain.RecurrenceRule
import java.time.ZonedDateTime

data class Block(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val hasRecipients: Boolean,
    val recurrenceCount: Int,
    val recurrenceRule: RecurrenceRule?
)