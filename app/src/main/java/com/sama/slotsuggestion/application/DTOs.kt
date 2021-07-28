package com.sama.slotsuggestion.application

import com.sama.slotsuggestion.domain.SlotSuggestion
import java.time.Duration
import java.time.ZoneId


data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val recipientTimezone: ZoneId,
    val suggestionCount: Int,
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>
)