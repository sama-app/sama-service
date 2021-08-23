package com.sama.slotsuggestion.domain.v2

import com.sama.common.DomainService
import com.sama.slotsuggestion.domain.v1.SlotSuggestion
import com.sama.slotsuggestion.domain.v1.sigmoid
import java.time.Duration
import kotlin.math.ceil


@DomainService
data class SlotSuggestionEngine(
    private val baseHeatMap: HeatMap,
) {
    fun suggest(duration: Duration, count: Int): Pair<List<SlotSuggestion>, HeatMap> {
        if (count == 0) {
            return emptyList<SlotSuggestion>() to baseHeatMap
        }

        val slotWindowSize = ceil(duration.toMinutes().toDouble() / baseHeatMap.intervalMinutes).toInt()
        val maxWeight = baseHeatMap.slots.maxOf { it.totalWeight }
        val sigmoidK = -10 / maxWeight

        var filteredHeatMap: HeatMap
        val suggestions = mutableListOf<SlotSuggestion>()
        do {
            // apply one-off weights (e.g. templates)
            filteredHeatMap = weigher {
                suggestedSlots(suggestions)
                thisOrNextWeek(suggestions)
            }.apply(baseHeatMap)

            val rankedSlots = filteredHeatMap.slots.asSequence()
                // Apply sigmoid, making sure that the highest weights produce values close to 1
                .map { sigmoid(x = it.totalWeight, k = sigmoidK) }
                // Create ranking for each slot of the specified duration
                .windowed(slotWindowSize) { it.reduce { acc, d -> acc * d } }
                .withIndex()

            // take the best suggestion
            val (index, score) = rankedSlots.maxByOrNull { it.value }!!

            val slotsInWindow = filteredHeatMap.slots.subList(index, index + slotWindowSize)
            val start = filteredHeatMap.slots[index].startDateTime.atZone(baseHeatMap.userTimeZone)
            val end = start.plus(duration)

            val bestSlot = SlotSuggestion(start, end, score, slotsInWindow)
            suggestions.add(bestSlot)

        } while (suggestions.size < count)

        return suggestions to filteredHeatMap
    }
}