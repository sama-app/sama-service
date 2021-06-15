package com.sama.slotsuggestion.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsRepository
import liquibase.pro.packaged.it
import org.springframework.stereotype.Service
import java.time.*

@ApplicationService
@Service
class SlotSuggestionService(
    private val userSettingsRepository: UserSettingsRepository,
    private val blockRepository: BlockRepository,
    private val heatMapConfiguration: HeatMapConfiguration
) {

    fun computeHistoricalHeatMap(userId: UserId): HistoricalHeapMap {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)

        val pastBlocksByDate = blockRepository.findAll(
            userId,
            ZonedDateTime.now(userSettings.timezone!!).minusDays(heatMapConfiguration.pastDays),
            ZonedDateTime.now(userSettings.timezone!!),
        )
            .filter { !it.allDay }
            .map { Block(it.startDateTime, it.endDateTime) }
            .groupBy { it.startDateTime.toLocalDate() }

        return HistoricalHeatMapGenerator(pastBlocksByDate)
            .generate()
    }

    fun computeFutureHeatMap(userId: UserId): FutureHeatMap {
        val historicalHeatMap = computeHistoricalHeatMap(userId)

        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        val dayWorkingHours = userSettings.dayWorkingHours
            .mapValues { it.value.workingHours }

        val futureBlocks = blockRepository.findAll(
            userId,
            LocalDate.now().atStartOfDay(userSettings.timezone),
            LocalDate.now().atStartOfDay(userSettings.timezone).plusDays(heatMapConfiguration.futureDays)
        )
            .map { Block(it.startDateTime, it.endDateTime) }
            .groupBy { it.startDateTime.toLocalDate() }

        return FutureHeatMapGenerator(
            historicalHeatMap, dayWorkingHours, futureBlocks, heatMapConfiguration.futureDays
        ).generate()
    }

    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): SlotSuggestionResponse {
        val futureHeatMap = computeFutureHeatMap(userId)

        val suggestions = SlotSuggestionEngine(futureHeatMap)
            .suggest(
                request.startDate,
                request.endDate,
                request.timezone,
                request.slotDuration,
                request.suggestionCount
            )

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