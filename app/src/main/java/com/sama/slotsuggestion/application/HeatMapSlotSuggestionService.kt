package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.integration.sentry.sentrySpan
import com.sama.slotsuggestion.domain.HeatMap
import com.sama.slotsuggestion.domain.SlotSuggestionEngine
import com.sama.slotsuggestion.domain.SuggestedSlotWeigher
import com.sama.slotsuggestion.domain.ThisOrNextWeekTemplateWeigher
import com.sama.users.domain.UserId
import java.time.Clock
import org.springframework.stereotype.Service

@ApplicationService
@Service
class HeatMapSlotSuggestionService(
    private val heatMapService: HeatMapService,
    private val clock: Clock
) : SlotSuggestionService {

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
        val initiatorHeatMap = heatMapService.generate(userId, null)
        val recipientHeatMap = heatMapService.generate(request.recipientId, null)
            .withTimeZone(clock, initiatorHeatMap.userTimeZone)

        val (user, recipient) = HeatMap.ensureDateRangesMatch(listOf(initiatorHeatMap, recipientHeatMap))

        sentrySpan(method = "SlotSuggestionEngine.suggest") {
            val (suggestions, _) = SlotSuggestionEngine(user, recipient)
                .suggest(
                    request.slotDuration,
                    request.suggestionCount,
                    listOf { s -> SuggestedSlotWeigher(s) }
                )

            return SlotSuggestionResponse(suggestions)
        }
    }
}