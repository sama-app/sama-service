package com.sama.slotsuggestion.domain.v2

import com.sama.meeting.application.MeetingSlotDTO
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.WorkingHours
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

    fun futureBlocks(block: Collection<Block>) {
        weighers.addAll(block.map { FutureBlockWeigher(it) })
    }

    fun workingHours(workingHours: Map<DayOfWeek, WorkingHours>) {
        weighers.add(WorkingHoursWeigher(workingHours))
    }

    fun recipientTimeZone(recipientTimeZone: ZoneId) {
        weighers.add(RecipientTimeZoneWeigher(recipientTimeZone))
    }

    fun futureProposedSlots(proposedSlots: Collection<MeetingSlotDTO>) {
        weighers.addAll(proposedSlots.map { FutureProposedSlotWeigher(it) })
    }

    fun searchBoundary() {
        weighers.add(SearchBoundaryWeigher())
    }

    fun recency() {
        weighers.add(RecencyWeigher())
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
