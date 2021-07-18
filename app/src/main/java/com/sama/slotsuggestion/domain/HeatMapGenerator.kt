package com.sama.slotsuggestion.domain

import com.sama.common.DomainEntity
import com.sama.common.DomainService
import com.sama.common.datesUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.streams.asSequence

@DomainEntity
data class HeatMap(
    private val value: Vector,
    val weightContext: WeightContext,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val timeZone: ZoneId,
) {

    fun vector(): Vector {
        return value.copyOf()
    }

    fun dayVectors(): Map<LocalDate, Vector> {
        return startDate.datesUntil(endDate).asSequence()
            .associateWith {
                val offset = startDate.until(it).days
                value.copyOfRange(
                    offset * weightContext.singleDayVectorSize,
                    (offset + 1) * weightContext.singleDayVectorSize
                )
            }
    }
}

@DomainService
data class HeatMapGenerator(val daysInFuture: Long, val weightContext: WeightContext) {
    private val startDate = LocalDate.now()
    private val endDate = LocalDate.now().plusDays(daysInFuture)

    fun generate(
        pastBlocks: Map<LocalDate, List<Block>>,
        futureBlocks: Map<LocalDate, List<Block>>,
        workingHours: Map<DayOfWeek, WorkingHours>,
        initiatorTimeZone: ZoneId,
        recipientTimeZone: ZoneId,
        searchDayCount: Int
    ): HeatMap {
        val (workdays, weekends) = pastBlocksWeights(pastBlocks)

        val baseWeights = startDate.datesUtil(endDate)
            .map { date ->
                // Create weights
                val pastBlockWeight = if (date.dayOfWeek.isWorkday()) workdays else weekends
                val workingHoursWeight = WorkingHoursWeigher(workingHours[date.dayOfWeek]).weigh(weightContext)
                val futureBlockWeights = futureBlocks[date]
                    ?.map { FutureBlockWeigher(it).weigh(weightContext) }
                    ?: emptyList()


                // Apply weights
                weightContext.zeroes()
                    .add(pastBlockWeight)
                    .add(workingHoursWeight)
                    .add(futureBlockWeights)
            }
            .reduce { acc, vector -> acc.plus(vector) }

        val finalWeights = baseWeights
            .add(SearchBoundaryWeigher(startDate, searchDayCount, initiatorTimeZone).weigh(weightContext))
            .add(RecipientTimeZoneWeigher(initiatorTimeZone, recipientTimeZone).weigh(weightContext))
            .add(RecencyWeigher().weigh(weightContext))

        return HeatMap(finalWeights, weightContext, startDate, endDate, initiatorTimeZone)
    }

    private fun pastBlocksWeights(pastBlocks: Map<LocalDate, List<Block>>): Pair<Vector, Vector> {
        val workdays = weightContext.zeroes()
        val weekends = weightContext.zeroes()

        pastBlocks.forEach { (date, blocks) ->
            val vector = if (date.isWorkday()) workdays else weekends
            blocks
                .map { PastBlockWeigher(it).weigh(weightContext) }
                .forEach { vector.add(it) }
        }

        return Pair(workdays, weekends)
    }
}


private fun LocalDate.isWorkday(): Boolean {
    return dayOfWeek.isWorkday()
}

private fun DayOfWeek.isWorkday(): Boolean {
    return this != DayOfWeek.SATURDAY && this != DayOfWeek.SUNDAY
}
