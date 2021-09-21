package com.sama.slotsuggestion.application

import com.sama.slotsuggestion.domain.SlotSuggestion
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.ZoneId

interface SlotSuggestionService {
    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse
    fun suggestSlots(userId: UserId, request: MultiUserSlotSuggestionRequest): SlotSuggestionResponse
}

data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val suggestionCount: Int,
    val recipientTimezone: ZoneId
)

data class MultiUserSlotSuggestionRequest(
    val slotDuration: Duration,
    val suggestionCount: Int,
    val recipientId: UserId,
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>,
)