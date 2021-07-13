package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsRepository
import org.springframework.stereotype.Service

@ApplicationService
@Service
class SlotSuggestionService(
    private val futureHeatMapService: FutureHeatMapService,
    private val userSettingsRepository: UserSettingsRepository
) {
    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val futureHeatMap = futureHeatMapService.find(userId)
        val calendarTimeZone = userSettingsRepository.findByIdOrThrow(userId).timezone!!

        val suggestions = SlotSuggestionEngine(futureHeatMap, calendarTimeZone)
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