package com.sama.slotsuggestion.domain

import com.sama.common.DomainEntity
import com.sama.common.DomainService
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

@JvmInline
@DomainEntity
value class UserHeapMap(val value: Map<DayOfWeek, Vector>)

@DomainService
data class UserHeatMapGenerator(val blocksByDate: Map<LocalDate, List<Block>>) {

    fun generate(): UserHeapMap {
        val workdays = ones()
        val weekends = ones()

        blocksByDate.forEach { (date, blocks) ->
            val vector = if (date.isWorkday()) workdays else weekends
            // dumb algorithm that adds 0.1 score for each existing meeting
            blocks.map { blockHeat(it) }
                .forEach { vector.add(it) }
        }

        val workdaysNormalized = workdays.normalize()
        val weekendsNormalized = weekends.normalize()

        return UserHeapMap(DayOfWeek.values()
            .associate {
                if (it.isWorkday()) {
                    it to workdaysNormalized
                } else {
                    it to weekendsNormalized
                }
            })
    }
}

fun LocalDate.isWorkday(): Boolean {
    return dayOfWeek.isWorkday()
}

fun DayOfWeek.isWorkday(): Boolean {
    return this != SATURDAY && this != SUNDAY
}

