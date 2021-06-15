package com.sama.slotsuggestion.domain

import com.sama.common.DomainEntity
import com.sama.common.DomainService
import com.sama.users.domain.WorkingHours
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import kotlin.streams.asSequence

@JvmInline
@DomainEntity
value class HistoricalHeapMap(val value: Map<DayOfWeek, Vector>)

@DomainService
data class HistoricalHeatMapGenerator(val pastBlocks: Map<LocalDate, List<Block>>) {

    fun generate(): HistoricalHeapMap {
        val workdays = zeroes()
        val weekends = zeroes()

        pastBlocks.forEach { (date, blocks) ->
            val vector = if (date.isWorkday()) workdays else weekends
            blocks.map { pastBlock(it) }
                .forEach { vector.add(it) }
        }

        return DayOfWeek.values()
            .associate {
                if (it.isWorkday()) {
                    it to workdays.copyOf()
                } else {
                    it to weekends.copyOf()
                }
            }
            .let { HistoricalHeapMap(it) }
    }
}

fun LocalDate.isWorkday(): Boolean {
    return dayOfWeek.isWorkday()
}

fun DayOfWeek.isWorkday(): Boolean {
    return this != SATURDAY && this != SUNDAY
}

