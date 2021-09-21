package com.sama.slotsuggestion.domain

import com.sama.common.DomainService
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.ceil

typealias SlotSuggestionWeigher = (suggestions: Collection<SlotSuggestion>) -> Weigher

data class SlotSuggestion(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val score: Double,
    val slots: List<Slot> = emptyList(),
)

@DomainService
data class SlotSuggestionEngine(private val baseHeatMap: HeatMap, private val overlayHeatMap: HeatMap? = null) {
    private val baseSigmoidK = computeSigmoidK(baseHeatMap)
    private val overlay = overlayHeatMap
        ?.let { heatMap ->
            val sigmoidK = computeSigmoidK(heatMap)
            heatMap.slots.asSequence()
                .map { sigmoid(x = it.totalWeight, k = sigmoidK) }
        }

    fun suggest(
        duration: Duration,
        count: Int,
        weighers: Collection<SlotSuggestionWeigher>,
    ): Pair<List<SlotSuggestion>, HeatMap> {
        if (count == 0) {
            return emptyList<SlotSuggestion>() to baseHeatMap
        }

        val slotWindowSize = ceil(duration.toMinutes().toDouble() / baseHeatMap.intervalMinutes).toInt()

        var filteredHeatMap: HeatMap
        val suggestions = mutableListOf<SlotSuggestion>()
        do {
            // apply one-off weights (e.g. templates)
            filteredHeatMap = weigher {
                weighers
                    .map { it.invoke(suggestions) }
                    .forEach { add(it) }
            }.apply(baseHeatMap)

            // Apply sigmoid, making sure that the highest weights produce values close to 1
            // and multiply with overlay if it is present
            var scoredSlots = filteredHeatMap.slots.asSequence()
                .map { sigmoid(x = it.totalWeight, k = baseSigmoidK) }
            scoredSlots = overlay?.zip(scoredSlots) { a, b -> a * b } ?: scoredSlots

            // Create ranking for each slot of the specified duration
            val rankedSlots = scoredSlots
                .windowed(slotWindowSize) { it.reduce { acc, d -> acc * d } }
                .withIndex()

            // take the best suggestion
            val (index, score) = rankedSlots.maxByOrNull { it.value }!!

            val slotsInWindow = baseHeatMap.slots.subList(index, index + slotWindowSize)
            val start = baseHeatMap.slots[index].startDateTime.atZone(baseHeatMap.userTimeZone)
            val end = start.plus(duration)

            val bestSlot = SlotSuggestion(start, end, score, slotsInWindow)
            suggestions.add(bestSlot)

        } while (suggestions.size < count)

        return suggestions to filteredHeatMap
    }

    private fun computeSigmoidK(heatMap: HeatMap): Double {
        val maxWeight = heatMap.slots.maxOf { it.totalWeight }
        return -10 / maxWeight
    }
}