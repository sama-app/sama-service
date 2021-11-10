package com.sama.slotsuggestion.application

import com.sama.users.application.InternalUserService
import com.sama.users.application.UserInternalDTO
import com.sama.users.domain.UserId
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TomasKeiSlotSuggestionService(
    private val userService: InternalUserService,
    private val defaultSlotSuggestionService: DefaultSlotSuggestionService
) : SlotSuggestionService {
    override fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val user = userService.findInternal(userId)
        return when {
            user.isKei() -> {
                val recipientId = userService.findIdsByEmail(setOf("tdirvonskas@gmail.com")).first()
                suggestSlots(
                    userId,
                    MultiUserSlotSuggestionRequest(
                        request.slotDuration,
                        request.suggestionCount,
                        recipientId
                    )
                )
            }
            user.isTomas() -> {
                val recipientId = userService.findIdsByEmail(setOf("kaji.keisuke@gmail.com")).first()
                suggestSlots(
                    userId,
                    MultiUserSlotSuggestionRequest(
                        request.slotDuration,
                        request.suggestionCount,
                        recipientId
                    )
                )
            }
            else -> defaultSlotSuggestionService.suggestSlots(userId, request)
        }
    }

    private fun UserInternalDTO.isTomas() = email == "tdirvonskas@gmail.com"
    private fun UserInternalDTO.isKei() = email == "kaji.keisuke@gmail.com"

    override fun suggestSlots(userId: UserId, request: MultiUserSlotSuggestionRequest) =
        defaultSlotSuggestionService.suggestSlots(userId, request)
}