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
    val historicalHeatMap: HistoricalHeatMap,
    val workingHours: Map<DayOfWeek, WorkingHours>,
    val futureBlocks: Map<LocalDate, List<Block>>,
    val daysInFuture: Long
) {

    fun generate(): FutureHeatMap {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now().plusDays(daysInFuture)

        return startDate.datesUntil(endDate).asSequence()
            .associateWith { date ->
                // Create weights
                val workingHoursWeights = workingHours(workingHours[date.dayOfWeek])

                val futureBlockWeights = futureBlocks[date]
                    ?.map { futureBlock(it) }
                    ?: emptyList()

                val historicalDataWeights = historicalHeatMap.value[date.dayOfWeek]!!

                // Apply weights
                zeroes()
                    .add(historicalDataWeights)
                    .add(workingHoursWeights)
                    .add(futureBlockWeights)
            }
            .let { FutureHeatMap(it) }
    }
}

