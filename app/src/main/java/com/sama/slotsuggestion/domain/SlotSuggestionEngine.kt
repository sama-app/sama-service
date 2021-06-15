package com.sama.slotsuggestion.domain

import com.sama.common.DomainService
import com.sama.common.mapValues
import com.sama.slotsuggestion.application.SlotSuggestion
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.streams.asSequence

@DomainService
data class SlotSuggestionEngine(val futureHeatMap: FutureHeatMap) {

    fun suggest(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        timezone: ZoneId,
        duration: Duration,
        count: Int
    ): List<SlotSuggestion> {

        val durationLength = ceil(duration.toMinutes().toDouble() / intervalMinutes).toInt()
        val startDate = startDateTime.toLocalDate()
        val endDate = endDateTime.toLocalDate()

        check(startDate in futureHeatMap.value && endDate in futureHeatMap.value)
        { "FutureHeapMap must contain queried date range" }

        return startDate.datesUntil(endDate).asSequence()
            .map { date -> futureHeatMap.value[date]!! }
            // Convert all days into one long vector
            .reduce { acc, vector -> acc.plus(vector) }
            // Apply date-based filter
            .add(searchBoundary(startDateTime, endDateTime))
            // Apply sigmoid
            .mapValues { sigmoid(it) }
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