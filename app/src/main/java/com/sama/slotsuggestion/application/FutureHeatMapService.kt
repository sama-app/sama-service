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
import java.time.LocalDate
import java.time.LocalTime
import kotlin.streams.asSequence

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
            .flatMap { block ->
                if (block.multiDay()) {
                    val zoneId = block.startDateTime.zone
                    val startDate = block.startDateTime.toLocalDate()
                    val endDate = block.endDateTime.toLocalDate()
                    startDate.datesUntil(endDate).asSequence()
                        .map {
                            when {
                                it.equals(startDate) -> {
                                    Block(
                                        block.startDateTime,
                                        LocalTime.MAX.atDate(it).atZone(zoneId),
                                        block.recipientEmail != null
                                    )
                                }
                                it.equals(endDate) -> {
                                    Block(
                                        LocalTime.MIN.atDate(it).atZone(zoneId),
                                        block.endDateTime,
                                        block.recipientEmail != null
                                    )
                                }
                                else -> {
                                    Block(
                                        LocalTime.MIN.atDate(it).atZone(zoneId),
                                        LocalTime.MAX.atDate(it).atZone(zoneId),
                                        block.recipientEmail != null
                                    )
                                }
                            }
                        }
                        .toList()
                } else {
                    listOf(Block(block.startDateTime, block.endDateTime, block.recipientEmail != null))
                }
            }
            .groupBy { it.startDateTime.toLocalDate() }

        return FutureHeatMapGenerator(
            historicalHeatMap, dayWorkingHours, futureBlocks, heatMapConfiguration.futureDays
        ).generate()
    }
}