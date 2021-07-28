package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class SlotSuggestionService(private val heatMapService: HeatMapService) {
    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val heatMap = heatMapService.generate(userId, request.recipientTimezone)

        val suggestions = SlotSuggestionEngine(heatMap)
            .suggest(
                request.slotDuration,
                request.suggestionCount
            )

        return SlotSuggestionResponse(suggestions)
    }
}