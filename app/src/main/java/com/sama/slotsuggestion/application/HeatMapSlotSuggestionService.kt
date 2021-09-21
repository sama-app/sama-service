package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.integration.sentry.sentrySpan
import com.sama.slotsuggestion.domain.SlotSuggestion
import com.sama.slotsuggestion.domain.SlotSuggestionEngine
import com.sama.slotsuggestion.domain.SlotSuggestionWeigher
import com.sama.slotsuggestion.domain.SuggestedSlotWeigher
import com.sama.slotsuggestion.domain.ThisOrNextWeekTemplateWeigher
import com.sama.slotsuggestion.domain.Weigher
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class HeatMapSlotSuggestionService(private val heatMapService: HeatMapService) : SlotSuggestionService {

    override fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val heatMap = heatMapService.generate(userId, request.recipientTimezone)

        sentrySpan(method = "SlotSuggestionEngine.suggest") {
            val (suggestions, _) = SlotSuggestionEngine(heatMap)
                .suggest(
                    request.slotDuration,
                    request.suggestionCount,
                    listOf(
                        { s -> SuggestedSlotWeigher(s) },
                        { s -> ThisOrNextWeekTemplateWeigher(s) }
                    )
                )

            return SlotSuggestionResponse(suggestions)
        }
    }

    override fun suggestSlots(userId: UserId, request: MultiUserSlotSuggestionRequest): SlotSuggestionResponse {
        val heatMap = heatMapService.generate(userId, null)
        val recipientHeatMap = heatMapService.generate(request.recipientId, null)

        sentrySpan(method = "SlotSuggestionEngine.suggest") {
            val (suggestions, _) = SlotSuggestionEngine(heatMap, recipientHeatMap)
                .suggest(
                    request.slotDuration,
                    request.suggestionCount,
                    listOf { s -> SuggestedSlotWeigher(s) }
                )

            return SlotSuggestionResponse(suggestions)
        }
    }
}