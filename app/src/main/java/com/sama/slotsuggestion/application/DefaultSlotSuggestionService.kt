package com.sama.slotsuggestion.application

import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class DefaultSlotSuggestionService(
    private val heatMapSlotSuggestionService: HeatMapSlotSuggestionService,
) : SlotSuggestionService {
    private var logger: Logger = LoggerFactory.getLogger(DefaultSlotSuggestionService::class.java)

    override fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        return try {
            heatMapSlotSuggestionService.suggestSlots(userId, request)
        } catch (e: Exception) {
            logger.error("slot suggestion v2 error", e)
            return SlotSuggestionResponse(emptyList())
        }
    }

    override fun suggestSlots(userId: UserId, request: MultiUserSlotSuggestionRequest): SlotSuggestionResponse {
        return try {
            heatMapSlotSuggestionService.suggestSlots(userId, request)
        } catch (e: Exception) {
            logger.error("multi-user slot suggestion v2 error", e)
            return SlotSuggestionResponse(emptyList())
        }
    }
}