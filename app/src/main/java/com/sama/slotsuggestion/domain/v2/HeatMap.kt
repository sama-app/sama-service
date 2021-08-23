package com.sama.slotsuggestion.domain.v2

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.chunkedBy
import com.sama.common.datesUtil
import com.sama.meeting.domain.MeetingSlot
import com.sama.users.domain.UserId
import java.time.*
import java.util.function.Predicate
import kotlin.math.pow

@DomainEntity
data class HeatMap(
    val userId: UserId,
    val userTimeZone: ZoneId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val intervalMinutes: Long,
    val slots: List<Slot>,
) {
    val interval = Duration.ofMinutes(intervalMinutes)

    companion object {
        @Factory
        fun create(
            userId: UserId,
            userTimeZone: ZoneId,
            startDate: LocalDate,
            endDate: LocalDate,
            intervalMinutes: Long,
        ): HeatMap {
            val slotCount = 24 * 60 / intervalMinutes
            val duration = Duration.ofMinutes(intervalMinutes)
            val slots = startDate.datesUtil(endDate)
                .flatMap { date ->
                    (0L until slotCount).map {
                        val startDateTime = LocalTime.MIN.plus(duration.multipliedBy(it)).atDate(date)
                        Slot(startDateTime, startDateTime.plus(duration))
                    }
                }
                .toList()

            return HeatMap(userId, userTimeZone, startDate, endDate, intervalMinutes, slots)
        }
    }

    fun normalize(): HeatMap {
        val totalWeights = slots.map { it.totalWeight }
        val median = (totalWeights[totalWeights.size / 2] + totalWeights[totalWeights.size / 2 + 1]) / 2

        return copy(
            slots = slots.map { it.addWeight(-median, "Normalisation") }
        )
    }
}

@DomainEntity
data class Slot(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val totalWeight: Double = 0.0,
    val influences: Map<Any, Double> = emptyMap(),
) {
    val dayOfWeek: DayOfWeek = startDateTime.dayOfWeek

    fun addWeight(weight: Double, tag: Any): Slot {
        return copy(
            totalWeight = totalWeight + weight,
            influences = influences.plus(tag to weight)
        )
    }
}

inline fun HeatMap.query(query: SlotQuery.() -> Unit): SlotQueryResult {
    val slotQuery = SlotQuery()
    query.invoke(slotQuery)
    return this.query(slotQuery)
}

fun HeatMap.query(predicate: Predicate<Slot>): SlotQueryResult {
    return slots.asSequence().mapIndexedNotNull { idx, slot ->
        if (predicate.test(slot)) {
            idx to slot
        } else {
            null
        }
    }
}

typealias TransformedSlots = Sequence<Pair<Int, Slot>>
typealias SlotQueryResult = Sequence<Pair<Int, Slot>>

inline fun SlotQueryResult.mapValue(crossinline transform: (Slot) -> Slot): TransformedSlots {
    return map { (idx, slot) -> idx to transform.invoke(slot) }
}

fun SlotQueryResult.addFixedWeight(weight: Double, tag: () -> String): TransformedSlots {
    return mapValue { slot -> slot.addWeight(weight, tag.invoke()) }
}

fun SlotQueryResult.addFixedWeight(weightTransform: (Slot) -> Double, tag: () -> String): TransformedSlots {
    return mapValue { slot -> slot.addWeight(weightTransform.invoke(slot), tag.invoke()) }
}

typealias GroupedSlotQueryResult = Sequence<List<Pair<Int, Slot>>>

data class SlotGroupInfo(val itemIdx: Int, val size: Int, val chunkIdx: Int)

/**
 * Returns a [Sequence] of query results where [Slot]s that are adjacent to one another are grouped
 * together. This allows to perform more complicated slot weight transformations, for example,
 * computing it dynamically based on the slot position in a group.
 */
fun SlotQueryResult.grouped(): GroupedSlotQueryResult {
    return chunkedBy { first, second -> first.first + 1 != second.first }
}

inline fun GroupedSlotQueryResult.mapValue(crossinline transform: (SlotGroupInfo, Slot) -> Slot): TransformedSlots {
    return flatMapIndexed { idx: Int, value: List<Pair<Int, Slot>> ->
        val chunkSize = value.size
        value.mapIndexed { itemIdx, item ->
            item.first to transform.invoke(
                SlotGroupInfo(itemIdx, chunkSize, idx),
                item.second
            )
        }
    }
}

@JvmName("addFixedWeightGrouped")
fun GroupedSlotQueryResult.addFixedWeight(weight: Double, tag: () -> String): TransformedSlots {
    return mapValue { _, slot -> slot.addWeight(weight, tag.invoke()) }
}

fun GroupedSlotQueryResult.addParabolicWeight(
    baseWeight: Double,
    weightStep: Double,
    tag: () -> String
): TransformedSlots {
    return mapValue { chunk, slot ->
        val itemIdx = chunk.itemIdx.toDouble()
        val chunkSize = chunk.size - 1
        val peakValue = weightStep * (chunkSize / 2)
        val fraction = (2.0 * (itemIdx / chunkSize) - 1).pow(2) // (2x - 1) ^ 2
        val weight = baseWeight + fraction * peakValue
        slot.addWeight(weight, tag.invoke())
    }
}

fun GroupedSlotQueryResult.addLinearWeight(
    startValue: Double = 0.0,
    endValue: Double = 0.0,
    tag: () -> String
): TransformedSlots {
    val valueDiff = endValue - startValue
    return mapValue { chunkInfo, slot ->
        val weightIncrement = valueDiff / (chunkInfo.size - 1)
        slot.addWeight(weightIncrement * chunkInfo.itemIdx, tag.invoke())
    }
}

fun Sequence<Pair<Int, Slot>>.save(heatMap: HeatMap): HeatMap {
    val newSlots = heatMap.slots.toMutableList()
    forEach { (idx, slot) -> newSlots[idx] = slot }
    return heatMap.copy(slots = newSlots)
}