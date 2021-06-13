package com.sama.suggest.domain

import com.sama.common.DomainService
import com.sama.suggest.application.SlotSuggestion
import com.sama.users.domain.WorkingHours
import liquibase.pro.packaged.it
import java.time.*
import kotlin.math.ceil
import kotlin.streams.asSequence

@DomainService
data class SlotSuggestionEngine(
    val userHeatMap: UserHeapMap,
    val dayWorkingHours: Map<DayOfWeek, WorkingHours>,
    val blocksByDate: Map<LocalDate, List<Block>>,
    val timezone: ZoneId
) {

    fun suggest(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        duration: Duration,
        count: Int
    ): List<SlotSuggestion> {
        val durationLength = ceil(duration.toMinutes().toDouble() / intervalMinutes).toInt()
        val startDate = startDateTime.toLocalDate()
        val endDate = endDateTime.toLocalDate()

        return startDate.datesUntil(endDate).asSequence()
            .map { date ->
                // Create masks
                val searchBoundaryMask = when {
                    date.isEqual(startDate) -> {
                        startTimeMask(startDateTime.toLocalTime())
                    }
                    date.isEqual(endDate) -> {
                        endTimeMask(endDateTime.toLocalTime())
                    }
                    else -> {
                        ones()
                    }
                }

                val workingHoursMask = workingHourMask(dayWorkingHours[date.dayOfWeek])

                val blockMasks = blocksByDate[date]
                    ?.map { blockMask(it) }
                    ?: emptyList()

                val userHeatMask = userHeatMap.value[date.dayOfWeek]!!

                // Apply masks
                ones()
                    .multiply(userHeatMask)
                    .multiply(searchBoundaryMask)
                    .multiply(workingHoursMask)
                    .multiply(blockMasks)
            }
            // Convert all days into one long vector
            .reduce { acc, mutableList -> acc.plus(mutableList) }
            // TODO: apply recency bias and any other "date-based" filters
            // TODO: move search boundary mask here
            // Create ranking for each slot of the specified duration
            .zipMultiplying(durationLength)
            // Sort by rank
            .mapIndexed { index, value -> index to value }
            .sortedByDescending { it.second }
            // Take the best suggestions, filtering out any zeroes
            .take(count)
            .filter { it.second != 0.0 }
            // Create resulting slots
            .map {
                val start = startDate.atStartOfDay(timezone).plus(indexToDurationOffset(it.first))
                val end = start.plus(duration)
                SlotSuggestion(start, end, it.second)
            }
            .toList()
    }
}