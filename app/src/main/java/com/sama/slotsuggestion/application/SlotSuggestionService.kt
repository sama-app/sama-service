package com.sama.slotsuggestion.application

import com.sama.slotsuggestion.domain.SlotSuggestion
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.ZoneId

interface SlotSuggestionService {
    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse
}

data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val recipientTimezone: ZoneId,
    val suggestionCount: Int,
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>,
)