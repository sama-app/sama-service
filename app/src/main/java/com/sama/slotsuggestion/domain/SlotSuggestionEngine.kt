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

        require(startDate in futureHeatMap.value && endDate in futureHeatMap.value)
        { "FutureHeapMap does not contain queried date range: $startDate - $endDate" }

        val computedVector = startDate.datesUntil(endDate).asSequence()
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
        val rankedVector = computedVector
            .mapIndexed { index, value -> index to value }
            .sortedByDescending { it.second }

        // Feed best slots into a filter to exclude overlapping slots
        val suggestionSlots = mutableListOf<Pair<Int, Double>>()
        for ((idx, rank) in rankedVector) {
            val isSlotOverlapping = suggestionSlots
                .filter {
                    val blockedRangeStart = it.first - durationLength + 1
                    val blockedRangeEnd = it.first + durationLength
                    idx in blockedRangeStart until blockedRangeEnd
                }
                .any()
            val isSlotAvailable = rank > 0

            if (!isSlotOverlapping && isSlotAvailable) {
                suggestionSlots.add(idx to rank)
            }

            if (suggestionSlots.size == count) {
                break
            }
        }

        return suggestionSlots
            .map {
                val start = startDate.atStartOfDay(timezone).plus(indexToDurationOffset(it.first))
                val end = start.plus(duration)
                SlotSuggestion(start, end, it.second)
            }
            .toList()
    }
}