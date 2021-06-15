package com.sama.calendar.domain

import java.time.ZonedDateTime
import java.util.*

/**
 * Represents a blocked part of a calendar
 */
data class Block(
    val blockId: BlockId,
    val externalId: String,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?
)