package com.sama.slotsuggestion.application

import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class DefaultSlotSuggestionService(
    private val slotSuggestionServiceV1: SlotSuggestionServiceV1,
    private val slotSuggestionServiceV2: SlotSuggestionServiceV2,
) : SlotSuggestionService {
    private var logger: Logger = LoggerFactory.getLogger(DefaultSlotSuggestionService::class.java)

    override fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        try {
            return slotSuggestionServiceV2.suggestSlots(userId, request)
        } catch (e: Exception) {
            logger.error("slot suggestion v2 error", e)
            return slotSuggestionServiceV1.suggestSlots(userId, request)
        }
    }
}