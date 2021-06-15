package com.sama.slotsuggestion.domain

import com.sama.common.DomainEntity
import com.sama.common.DomainService
import com.sama.users.domain.WorkingHours
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.streams.asSequence

@JvmInline
@DomainEntity
value class FutureHeatMap(val value: Map<LocalDate, Vector>)

@DomainService
data class FutureHeatMapGenerator(
    val historicalHeatMap: HistoricalHeapMap,
    val workingHours: Map<DayOfWeek, WorkingHours>,
    val futureBlocks: Map<LocalDate, List<Block>>,
    val daysInFuture: Long
) {

    fun generate(): FutureHeatMap {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now().plusDays(daysInFuture)

        return startDate.datesUntil(endDate).asSequence()
            .associateWith { date ->
                // Create masks
                val workingHoursMask = workingHours(workingHours[date.dayOfWeek])

                val blockMasks = futureBlocks[date]
                    ?.map { futureBlock(it) }
                    ?: emptyList()

                val historicalDataMask = historicalHeatMap.value[date.dayOfWeek]!!

                // Apply masks
                zeroes()
                    .add(historicalDataMask)
                    .add(workingHoursMask)
                    .add(blockMasks)
            }
            .let { FutureHeatMap(it) }
    }
}

