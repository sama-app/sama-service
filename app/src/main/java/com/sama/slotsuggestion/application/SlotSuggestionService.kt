package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class SlotSuggestionService(
    private val futureHeatMapService: FutureHeatMapService
) {
    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val futureHeatMap = futureHeatMapService.find(userId)

        val suggestions = SlotSuggestionEngine(futureHeatMap)
            .suggest(
                request.startDateTime,
                request.endDateTime,
                request.timezone,
                request.slotDuration,
                request.suggestionCount
            )

        return SlotSuggestionResponse(suggestions)
    }
}