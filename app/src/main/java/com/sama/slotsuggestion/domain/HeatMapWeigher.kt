package com.sama.slotsuggestion.domain

import com.sama.meeting.application.MeetingSlotDTO
import java.time.DayOfWeek
import java.time.ZoneId


inline fun weigher(query: HeatMapWeigherBuilder.() -> Unit): HeatMapWeigher {
    val builder = HeatMapWeigherBuilder()
    query.invoke(builder)
    return builder.build()
}

data class HeatMapWeigherBuilder(
    val weighers: MutableList<Weigher> = mutableListOf(),
) {
    fun pastBlocks(block: Collection<Block>) {
        weighers.addAll(block.map { PastBlockWeigher(it) })
    }

    fun futureBlocks(blocks: Collection<Block>) {
        weighers.add(FutureBlocksWeigher(blocks))
    }

    fun workingHours(workingHours: Map<DayOfWeek, WorkingHours>) {
        weighers.add(WorkingHoursWeigher(workingHours))
    }

    fun recipientTimeZone(recipientTimeZone: ZoneId) {
        weighers.add(RecipientTimeZoneWeigher(recipientTimeZone))
    }

    fun futureProposedSlots(proposedSlots: List<MeetingSlotDTO>) {
        weighers.add(FutureProposedSlotWeigher(proposedSlots))
    }

    fun searchBoundary() {
        weighers.add(SearchBoundaryWeigher())
    }

    fun recency() {
        weighers.add(RecencyWeigher())
    }

    fun suggestedSlots(suggestions: Collection<SlotSuggestion>) {
        weighers.add(SuggestedSlotWeigher(suggestions))
    }

    fun thisOrNextWeek(suggestions: Collection<SlotSuggestion>) {
        weighers.add(ThisOrNextWeekTemplateWeigher(suggestions))
    }

    fun build(): HeatMapWeigher {
        return HeatMapWeigher(weighers)
    }
}

data class HeatMapWeigher(val weighers: List<Weigher>) {
    fun apply(baseHeatMap: HeatMap): HeatMap {
        var result = baseHeatMap
        for (weigher in weighers) {
            result = weigher.weight(result)
        }
        return result
    }
}
