package com.sama.suggest.domain

import com.sama.users.domain.WorkingHours
import java.time.Duration
import java.time.LocalTime
import kotlin.math.ceil

/**
 * Duration of each slot in the day to consider. E.g. if the value is 15
 * then a day is divided in 96 segments.
 */
const val intervalMinutes = 15
const val defaultSize = 24 * 60 / intervalMinutes

fun zeroes(): Vector {
    return zeroes(defaultSize)
}

fun ones(): Vector {
    return ones(defaultSize)
}

fun timeToIndex(time: LocalTime): Int {
    return ceil((time.hour * 60 + time.minute).toDouble() / intervalMinutes).toInt()
}

fun indexToDurationOffset(index: Int): Duration {
    return Duration.ofMinutes((index * intervalMinutes).toLong())
}

fun startTimeMask(time: LocalTime): Vector {
    return cliff(0.0, 1.0, defaultSize, timeToIndex(time), defaultSize)
}

fun endTimeMask(time: LocalTime): Vector {
    return cliff(1.0, 0.0, defaultSize, 0, timeToIndex(time))
}

fun workingHourMask(workingHours: WorkingHours?): Vector {
    if (workingHours == null) {
        return zeroes(defaultSize)
    }
    if (workingHours.isAllDay()) {
        return ones(defaultSize)
    }

    val startTimeIndex = timeToIndex(workingHours.startTime)
    val endTimeIndex = timeToIndex(workingHours.endTime)

    return sigmoidSlope(0.0, 1.0, defaultSize, startTimeIndex, endTimeIndex, -1 to 2)
}

fun blockMask(block: Block?): Vector {
    if (block == null) {
        return ones(defaultSize)
    }
    val startTimeIndex = timeToIndex(block.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(block.endDateTime.toLocalTime())
    return sigmoidSlope(1.0, 0.0, defaultSize, startTimeIndex, endTimeIndex, -2 to 0)
}

fun blockHeat(block: Block?): Vector {
    if (block == null) {
        return zeroes(defaultSize)
    }
    val startTimeIndex = timeToIndex(block.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(block.endDateTime.toLocalTime())

    return cliff(0.0, 0.1, defaultSize, startTimeIndex, endTimeIndex)
}
