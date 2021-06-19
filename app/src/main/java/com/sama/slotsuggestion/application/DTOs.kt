package com.sama.slotsuggestion.application

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val timezone: ZoneId,
    val suggestionCount: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>
)

data class SlotSuggestion(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val score: Double
)