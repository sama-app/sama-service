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
    fun suggest(duration: Duration, count: Int): List<SlotSuggestion> {
        if (count == 0) {
            return emptyList()
        }

        val slotWindowSize = ceil(duration.toMinutes().toDouble() / baseHeatMap.intervalMinutes).toInt()
        val maxWeight = baseHeatMap.slots.maxOf { it.totalWeight }

        var heatMap = baseHeatMap
        val suggestions = mutableListOf<SlotSuggestion>()
        do {
            val rankedSlots = heatMap.slots
                // Apply sigmoid, making sure that the highest weights produce values close to 1
                .map { sigmoid(x = it.totalWeight, k = -10 / maxWeight) }
                // Create ranking for each slot of the specified duration
                .windowed(slotWindowSize)
                .mapIndexed { idx, it -> idx to it.reduce { acc, d -> acc * d } }

            // take the best suggestion
            val (index, score) = rankedSlots.maxByOrNull { it.second }!!

            val slotsInWindow = heatMap.slots.subList(index, index + slotWindowSize)
            val start = heatMap.slots[index].startDateTime.atZone(baseHeatMap.userTimeZone)
            val end = start.plus(duration)

            val bestSlot = SlotSuggestion(start, end, score, slotsInWindow)
            suggestions.add(bestSlot)

            // update heatmap to not suggest the same slot again
            heatMap = SuggestedSlotWeigher(bestSlot)
                .weight(heatMap)

        } while (suggestions.size < count)

        return suggestions
    }
}