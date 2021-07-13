package com.sama.slotsuggestion.domain

import com.sama.common.DomainService
import com.sama.common.mapValues
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.streams.asSequence

data class SlotSuggestion(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val score: Double
)

@DomainService
data class SlotSuggestionEngine(private val futureHeatMap: FutureHeatMap) {
    private val startDate = futureHeatMap.value.keys.minOrNull()!!
    private val endDate = futureHeatMap.value.keys.maxOrNull()!!

    fun suggest(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        timezone: ZoneId,
        duration: Duration,
        count: Int
    ): List<SlotSuggestion> {
        val durationLength = ceil(duration.toMinutes().toDouble() / intervalMinutes).toInt()
        val multiDayWeights = listOf(
            searchBoundary(startDate, endDate, startDateTime, endDateTime),
            recency(startDate, endDate)
        )

        val suggestions = mutableListOf<SlotSuggestion>()
        do {
            val rankedVector = rankedVector(multiDayWeights, durationLength, suggestions)
            // take the best suggestion
            val bestSuggestion = rankedVector.first()
                .let {
                    val start = startDate.atStartOfDay(initiatorTimeZone)
                        .plus(indexToDurationOffset(it.first))
                    val end = start.plus(duration)
                    SlotSuggestion(start, end, it.second)
                }
            suggestions.add(bestSuggestion)
        } while (suggestions.size < count)

        return suggestions
    }

    private fun rankedVector(
        multiDayWeights: List<Vector>,
        durationSlotCount: Int,
        suggestions: List<SlotSuggestion>
    ): List<Pair<Int, Double>> {
        // create weights for currently suggested slots
        val suggestionWeightsByDate = suggestions
            .groupBy { it.startDateTime.toLocalDate() }
            .mapValues { entry ->
                entry.value
                    .map { suggestedSlot(it) }
                    .reduce { acc, vector -> acc.add(vector) }
            }

        // Create unranked vector with all weights applied
        val fullHeatMap = startDate.datesUntil(endDate).asSequence()
            // Apply weights for currently suggested slots to exclude
            // overlaps and allow for better suggestion logic
            .map { date ->
                val suggestionWeights = suggestionWeightsByDate[date] ?: zeroes()
                val dayVector = futureHeatMap.value[date]!! // note: this is mutable!
                suggestionWeights.add(dayVector)
            }
            // Convert all days into one long vector
            .reduce { acc, vector -> acc.plus(vector) }
            // Apply multi-day weights
            .add(multiDayWeights)

        // Compute ranked vector
        val rankedVector = fullHeatMap
            // Apply sigmoid
            .mapValues { sigmoid(it) }
            // Create ranking for each slot of the specified duration
            .zipMultiplying(durationSlotCount)
            // Sort by rank
            .mapIndexed { index, value -> index to value }
            .sortedByDescending { it.second }

        return rankedVector
    }
}