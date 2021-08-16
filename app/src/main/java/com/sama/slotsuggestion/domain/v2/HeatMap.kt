package com.sama.slotsuggestion.domain.v2

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.datesUtil
import com.sama.users.domain.UserId
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.function.Predicate

@DomainEntity
data class HeatMap(
    val userId: UserId,
    val userTimeZone: ZoneId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val intervalMinutes: Long,
    val slots: List<Slot>,
) {
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
                        Slot(startDateTime, startDateTime.plus(duration), 0.0, emptyMap())
                    }
                }
                .toList()

            return HeatMap(userId, userTimeZone, startDate, endDate, intervalMinutes, slots)
        }
    }
}

@DomainEntity
data class Slot(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val totalWeight: Double,
    val influences: Map<Any, Double>,
) {
    val dayOfWeek: DayOfWeek = startDateTime.dayOfWeek

    fun addWeight(influence: Any, weight: Double): Slot {
        return copy(
            totalWeight = totalWeight + weight,
            influences = influences.plus(influence to weight)
        )
    }
}

inline fun HeatMap.query(query: SlotQuery.() -> Unit): Sequence<Pair<Int, Slot>> {
    val slotQuery = SlotQuery()
    query.invoke(slotQuery)
    return this.query(slotQuery)
}

fun HeatMap.query(predicate: Predicate<Slot>): Sequence<Pair<Int, Slot>> {
    return slots.asSequence().mapIndexedNotNull { idx, slot ->
        if (predicate.test(slot)) {
            idx to slot
        } else {
            null
        }
    }
}

inline fun Sequence<Pair<Int, Slot>>.modify(crossinline transform: (Int, Slot) -> Slot): Sequence<Pair<Int, Slot>> {
    return map { (idx, slot) -> idx to transform.invoke(idx, slot) }
}

fun Sequence<Pair<Int, Slot>>.save(heatMap: HeatMap): HeatMap {
    val newSlots = heatMap.slots.toMutableList()
    forEach { (idx, slot) -> newSlots[idx] = slot }
    return heatMap.copy(slots = newSlots)
}