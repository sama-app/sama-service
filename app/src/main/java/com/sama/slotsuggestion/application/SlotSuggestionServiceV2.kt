package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.integration.sentry.sentrySpan
import com.sama.slotsuggestion.domain.v2.SlotSuggestionEngine
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class SlotSuggestionServiceV2(private val heatMapService: HeatMapServiceV2) : SlotSuggestionService {

    override fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val heatMap = heatMapService.generate(userId, request.recipientTimezone)

        sentrySpan(method = "SlotSuggestionEngine.suggest") {
            val (suggestions, _) = SlotSuggestionEngine(heatMap)
                .suggest(
                    request.slotDuration,
                    request.suggestionCount
                )

            return SlotSuggestionResponse(suggestions)
        }
    }
}