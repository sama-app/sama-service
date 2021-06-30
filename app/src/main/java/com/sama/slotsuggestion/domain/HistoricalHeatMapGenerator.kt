package com.sama.slotsuggestion.domain

import com.sama.common.DomainEntity
import com.sama.common.DomainService
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

@JvmInline
@DomainEntity
value class HistoricalHeatMap(val value: Map<DayOfWeek, Vector>)

@DomainService
data class HistoricalHeatMapGenerator(val pastBlocks: Map<LocalDate, List<Block>>) {

    fun generate(): HistoricalHeatMap {
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
            .let { HistoricalHeatMap(it) }
    }
}

fun LocalDate.isWorkday(): Boolean {
    return dayOfWeek.isWorkday()
}

fun DayOfWeek.isWorkday(): Boolean {
    return this != SATURDAY && this != SUNDAY
}

