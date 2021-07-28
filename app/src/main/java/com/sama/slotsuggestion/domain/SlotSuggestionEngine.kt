package com.sama.slotsuggestion.domain

import com.sama.common.DomainService
import com.sama.common.mapValues
import java.time.Duration
import java.time.ZonedDateTime

data class SlotSuggestion(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val score: Double
)

@DomainService
data class SlotSuggestionEngine(
    private val baseHeatMap: HeatMap
) {
    private val startDate = baseHeatMap.startDate
    private val weightContext = baseHeatMap.weightContext

    fun suggest(duration: Duration, count: Int): List<SlotSuggestion> {
        if (count == 0) {
            return emptyList()
        }

        val heatMap = baseHeatMap.vector()

        val searchSlotCount = weightContext.durationToOffset(duration)

        val suggestions = mutableListOf<SlotSuggestion>()
        do {
            // take the best suggestion
            val (index, score) = heatMap.copyOf()
                // Apply sigmoid
                .mapValues { sigmoid(it) }
                // Create ranking for each slot of the specified duration
                .zipMultiplying(searchSlotCount)
                // Sort by score
                .mapIndexed { index, value -> index to value }
                .maxByOrNull { it.second }!!

            val start = startDate.atStartOfDay(baseHeatMap.timeZone)
                .plus(weightContext.indexToDurationOffset(index))
            val end = start.plus(duration)

            val bestSlot = SlotSuggestion(start, end, score)
            suggestions.add(bestSlot)

            // update heatmap to not suggest the same slot again
            val bestSlotWeight = SuggestedSlotWeigher(bestSlot, startDate)
                .weigh(weightContext)
            heatMap.add(bestSlotWeight)

        } while (suggestions.size < count)

        return suggestions
    }
}