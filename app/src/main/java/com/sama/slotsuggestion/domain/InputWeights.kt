package com.sama.slotsuggestion.domain

import com.sama.calendar.domain.Recurrence
import com.sama.users.domain.WorkingHours
import java.time.*
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

fun indexToDurationOffset(index: Int): Duration {
    return Duration.ofMinutes((index * intervalMinutes).toLong())
}

fun minutesToSlotOffset(minutes: Int): Int {
    return ceil(minutes.toDouble() / intervalMinutes).toInt()
}

/**
 * @return a day [Vector] that handles the value of slots for a block time span in the past
 */
fun pastBlock(block: Block?): Vector {
    if (block == null) {
        return zeroes(vectorSize)
    }
    val startTimeIndex = timeToIndex(block.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(block.endDateTime.toLocalTime())

    val weight = if (block.hasRecipients) {
        // there's a meeting with a person, we should add weight
        0.25
    } else {
        // if it's a self-blocked time, we count as we DON'T want meetings for the time
        -0.25
    } / block.recurrenceCount

    return cliff(0.0, weight, vectorSize, startTimeIndex, endTimeIndex)
}

/**
 * @return a day [Vector] that weights down slots outside the working hours
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

    return linearCurve(-10.0, 0.0, vectorSize, startTimeIndex, endTimeIndex, -1 to 2)
}

/**
 * @return a day [Vector] that weights down slots for a block time span
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
 * @return a day [Vector] that weights down previously suggested slots
 */
fun suggestedSlot(slot: SlotSuggestion?): Vector {
    if (slot == null) {
        return zeroes(vectorSize)
    }
    val startTimeIndex = timeToIndex(slot.startDateTime.toLocalTime())
    val endTimeIndex = timeToIndex(slot.endDateTime.toLocalTime())
    return linearCurve(0.0, -100.0, vectorSize, startTimeIndex, endTimeIndex, -4 to 0)
}

/**
 * @return a multi-day [Vector] that weights down slots that are outside the given date time range
 */
fun searchBoundary(
    startDate: LocalDate,
    endDate: LocalDate,
    searchFrom: LocalDateTime,
    searchTo: LocalDateTime
): Vector {
    return startDate.datesUntil(endDate).asSequence()
        .map { date ->
            when {
                date.isEqual(startDate) -> {
                    cliff(-100.0, 0.0, vectorSize, timeToIndex(searchFrom.toLocalTime()), vectorSize)
                }
                date.isEqual(endDate) -> {
                    cliff(0.0, -100.0, vectorSize, 0, timeToIndex(searchTo.toLocalTime()))
                }
                else -> {
                    zeroes()
                }
            }
        }
        .reduce { acc, vector -> acc.plus(vector) }
}

/**
 * @return a multi-day [Vector] that weights down slots that are in the future
 */
fun recency(startDate: LocalDate, endDate: LocalDate): Vector {
    val days = startDate.until(endDate).days
    val multiDayVectorSize = days * vectorSize
    val vector = curve(
        -1.0,
        0.5,
        multiDayVectorSize,
        0,
        vectorSize,
        0 to 0, { x -> x },
        -multiDayVectorSize + vectorSize to 0, { x -> x }
    )
    return vector
}

/**
 * @return a multi-day [Vector] that weights down slots that conflict with the requestTimeZone
 * assuming a reasonable 8:00 - 20:00 "working hours" of the recipient
 */
fun recipientTimeZone(
    startDate: LocalDate,
    endDate: LocalDate,
    userTimeZone: ZoneId,
    requestTimeZone: ZoneId
): Vector {
    val dayCount = startDate.datesUntil(endDate).count().toInt()

    val now = LocalDateTime.now()
    val userOffsetSeconds = userTimeZone.rules.getOffset(now).totalSeconds
    val requestOffsetSeconds = requestTimeZone.rules.getOffset(now).totalSeconds
    if (userOffsetSeconds == requestOffsetSeconds) {
        return zeroes(vectorSize * dayCount)
    }

    val startTimeIndex = timeToIndex(LocalTime.of(8, 0))
    val endTimeIndex = timeToIndex(LocalTime.of(20, 0))

    val offsetMinutes = (userOffsetSeconds - requestOffsetSeconds) / 60
    val slotOffset = minutesToSlotOffset(offsetMinutes)

    return startDate.datesUntil(endDate).asSequence()
        .map { linearCurve(-3.0, 0.0, vectorSize, startTimeIndex, endTimeIndex, -1 to 2) }
        .reduce { acc, vector -> acc.plus(vector) }
        .rotate(slotOffset)
}