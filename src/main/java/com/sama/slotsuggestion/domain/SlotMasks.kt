package com.sama.slotsuggestion.domain

import com.sama.users.domain.WorkingHours
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.streams.asSequence

/**
 * Duration of each slot in the day to consider. E.g. if the value is 15
 * then a day is divided into 96 segments.
 */
const val intervalMinutes = 15
const val vectorSize = 24 * 60 / intervalMinutes

fun zeroes(): Vector {
    return zeroes(vectorSize)
}

fun ones(): Vector {
    return ones(vectorSize)
}

fun timeToIndex(time: LocalTime): Int {
    return ceil((time.hour * 60 + time.minute).toDouble() / intervalMinutes).toInt()
}

/**
 * @return a [Duration]
 */
fun indexToDurationOffset(index: Int): Duration {
    return Duration.ofMinutes((index * intervalMinutes).toLong())
}

/**
 * @return a day [Vector] that increases the value of slots for a block time span
 */
fun pastBlock(block: Block?): Vector {
    if (block == null) {
        return zeroes(vectorSize)
    }
    val startTimeIndex = timeToIndex(block.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(block.endDateTime.toLocalTime())

    return cliff(0.0, 1.0, vectorSize, startTimeIndex, endTimeIndex)
}

/**
 * @return a day [Vector] that weight down slots outside the working hours
 */
fun workingHours(workingHours: WorkingHours?): Vector {
    if (workingHours == null) {
        return line(vectorSize, -10.0)
    }
    if (workingHours.isAllDay()) {
        return zeroes(vectorSize)
    }

    val startTimeIndex = timeToIndex(workingHours.startTime)
    val endTimeIndex = timeToIndex(workingHours.endTime)

    return linearSlope(-10.0, 0.0, vectorSize, startTimeIndex, endTimeIndex, -1 to 2)
}

/**
 * @return a day [Vector] that weight down slots for a block time span
 */
fun futureBlock(block: Block?): Vector {
    if (block == null) {
        return ones(vectorSize)
    }
    val startTimeIndex = timeToIndex(block.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(block.endDateTime.toLocalTime())
    return cliff(0.0, -100.0, vectorSize, startTimeIndex, endTimeIndex)
}

/**
 * @return a multi-day [Vector] that weights down slots that are outside the given date time range
 */
fun searchBoundary(from: LocalDateTime, to: LocalDateTime): Vector {
    val startDate = from.toLocalDate()
    val endDate = to.toLocalDate()

    return startDate.datesUntil(endDate).asSequence()
        .map { date ->
            when {
                date.isEqual(startDate) -> {
                    cliff(-100.0, 0.0, vectorSize, timeToIndex(from.toLocalTime()), vectorSize)
                }
                date.isEqual(endDate) -> {
                    cliff(0.0, -100.0, vectorSize, 0, timeToIndex(to.toLocalTime()))
                }
                else -> {
                    zeroes()
                }
            }
        }
        .reduce { acc, vector -> acc.plus(vector) }
}