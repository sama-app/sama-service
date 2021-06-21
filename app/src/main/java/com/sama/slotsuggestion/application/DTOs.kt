package com.sama.slotsuggestion.application

import com.sama.slotsuggestion.domain.SlotSuggestion
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId


data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val timezone: ZoneId,
    val suggestionCount: Int,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>
)