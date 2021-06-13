package com.sama.suggest.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.suggest.domain.Block
import com.sama.suggest.domain.SlotSuggestionEngine
import com.sama.suggest.domain.UserHeapMap
import com.sama.suggest.domain.UserHeatMapGenerator
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ApplicationService
@Service
class SlotSuggestionService(
    private val userSettingsRepository: UserSettingsRepository,
    private val blockRepository: BlockRepository
) {

    fun computeHeapMap(userId: UserId): UserHeapMap {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)

        // Generate HeatMap
        val pastBlocksByDate = blockRepository.findAll(
            userId,
            ZonedDateTime.now(userSettings.timezone!!).minusDays(90),
            ZonedDateTime.now(userSettings.timezone!!),
        )
            .filter { !it.allDay }
            .map { Block(it.startDateTime, it.endDateTime) }
            .groupBy { it.startDateTime.toLocalDate() }

        return UserHeatMapGenerator(pastBlocksByDate)
            .generate()
    }

    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)

        // Generate HeatMap
        val pastBlocksByDate = blockRepository.findAll(
            userId,
            request.startDate.atZone(userSettings.timezone!!).minusDays(90),
            request.startDate.atZone(userSettings.timezone!!)
        )
            .filter { !it.allDay }
            .map { Block(it.startDateTime, it.endDateTime) }
            .groupBy { it.startDateTime.toLocalDate() }

        val userHeatMap = UserHeatMapGenerator(pastBlocksByDate)
            .generate()

        // Generate suggestions
        val dayWorkingHours = userSettings.dayWorkingHours
            .mapValues { it.value.workingHours }

        val futureBlocksByDate = blockRepository.findAll(
            userId,
            request.startDate.atZone(request.timezone),
            request.endDate.atZone(request.timezone)
        )
            .map { Block(it.startDateTime, it.endDateTime) }
            .groupBy { it.startDateTime.toLocalDate() }

        val suggestions = SlotSuggestionEngine(userHeatMap, dayWorkingHours, futureBlocksByDate, request.timezone)
            .suggest(request.startDate, request.endDate, request.slotDuration, request.suggestionCount)

        return SlotSuggestionResponse(suggestions)
    }
}

data class SlotSuggestionRequest(
    val slotDuration: Duration,
    val timezone: ZoneId,
    val suggestionCount: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class SlotSuggestionResponse(
    val suggestions: List<SlotSuggestion>
)

data class SlotSuggestion(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val score: Double
)