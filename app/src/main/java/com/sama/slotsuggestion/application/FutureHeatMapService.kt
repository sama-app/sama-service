package com.sama.slotsuggestion.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@ApplicationService
@Service
class FutureHeatMapService(
    private val historicalHeatMapService: HistoricalHeatMapService,
    private val userSettingsRepository: UserSettingsRepository,
    private val blockRepository: BlockRepository,
    private val heatMapConfiguration: HeatMapConfiguration,
) {
    fun find(userId: UserId): FutureHeatMap {
        val historicalHeatMap = historicalHeatMapService.find(userId)

        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        val dayWorkingHours = userSettings.dayWorkingHours
            .mapValues { it.value.workingHours }

        val futureBlocks = blockRepository.findAll(
            userId,
            LocalDate.now().atStartOfDay(userSettings.timezone),
            LocalDate.now().atStartOfDay(userSettings.timezone).plusDays(heatMapConfiguration.futureDays)
        )
            .map { Block(it.startDateTime, it.endDateTime, it.recipientEmail != null) }
            .groupBy { it.startDateTime.toLocalDate() }

        return FutureHeatMapGenerator(
            historicalHeatMap, dayWorkingHours, futureBlocks, heatMapConfiguration.futureDays
        ).generate()
    }
}