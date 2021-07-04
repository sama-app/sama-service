package com.sama.slotsuggestion.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.HistoricalHeatMap
import com.sama.slotsuggestion.domain.HistoricalHeatMapGenerator
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@ApplicationService
@Service
class HistoricalHeatMapService(
    private val userSettingsRepository: UserSettingsRepository,
    private val blockRepository: BlockRepository,
    private val heatMapConfiguration: HeatMapConfiguration
) {

    @Cacheable("historical-heat-map")
    fun find(userId: UserId): HistoricalHeatMap {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)

        val pastBlocksByDate = blockRepository.findAll(
            userId,
            ZonedDateTime.now(userSettings.timezone!!).minusDays(heatMapConfiguration.historicalDays),
            ZonedDateTime.now(userSettings.timezone!!),
        )
            .filter { !it.allDay && !it.multiDay() }
            .map { Block(it.startDateTime, it.endDateTime, it.recipientEmail != null) }
            .groupBy { it.startDateTime.toLocalDate() }

        return HistoricalHeatMapGenerator(pastBlocksByDate)
            .generate()
    }
}