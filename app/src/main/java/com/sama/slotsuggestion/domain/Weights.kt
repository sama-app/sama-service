package com.sama.slotsuggestion.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.streams.asSequence

interface Weigher {
    fun weigh(weightContext: WeightContext): Vector
}

@JvmInline
value class PastBlockWeigher(private val block: Block?) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        if (block == null || block.multiDay() || block.allDay) {
            return weightContext.zeroes()
        }

        val startTime = block.startDateTime.toLocalTime()
        val endTime = block.endDateTime.toLocalTime()

        val weight = if (block.hasRecipients) {
            // there's a meeting with a person, we should add weight
            0.05
        } else {
            // if it's a self-blocked time, we count as we DON'T want meetings for the time
            -0.1
        } / block.recurrenceCount

        return weightContext.cliff(startTime, endTime, 0.0, weight)
    }
}

@JvmInline
value class FutureBlockWeigher(private val block: Block?) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        if (block == null) {
            return weightContext.ones()
        }
        val startTime = block.startDateTime.toLocalTime()
        val endTime = block.endDateTime.toLocalTime()

        val weight = weightContext.cliff(startTime, endTime, 0.0, -100.0)

        // Prefer meetings close to an existing one
        val startTimeIndex = weightContext.timeToIndex(startTime)
        if (startTimeIndex >= 1) {
            weight[startTimeIndex - 1] += 0.1
        }
        val endTimeIndex = weightContext.timeToIndex(endTime)
        if (endTimeIndex <= weight.size - 1) {
            weight[endTimeIndex] += 0.1
        }

        return weight
    }
}

@JvmInline
value class WorkingHoursWeigher(private val wh: WorkingHours?) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        if (wh == null) {
            return weightContext.line(-5.0)
        }
        if (wh.isAllDay()) {
            return weightContext.zeroes()
        }

        return weightContext.linearCurve(wh.startTime, wh.endTime, -5.0, 0.0, -4 to 0)
    }
}

data class SearchBoundaryWeigher(
    private val searchStartDate: LocalDate,
    private val suggestionDayCount: Int,
    private val initiatorTimeZone: ZoneId
) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        val endDate = searchStartDate.plusDays(weightContext.days.toLong())
        val searchEndDate = searchStartDate.plusDays(suggestionDayCount.toLong())
        return searchStartDate.datesUntil(endDate).asSequence()
            .map { date ->
                when {
                    date.isEqual(searchStartDate) -> {
                        val startTime = LocalDateTime.now(initiatorTimeZone).toLocalTime()
                        weightContext.cliff(startTime, LocalTime.MAX, -100.0, 0.0)
                    }
                    date.isBefore(searchEndDate) -> {
                        weightContext.zeroes()
                    }
                    date.isEqual(searchEndDate) -> {
                        val endTime = LocalDateTime.now(initiatorTimeZone)
                            .plusDays(suggestionDayCount.toLong()).toLocalTime()
                        weightContext.cliff(LocalTime.MIDNIGHT, endTime, 0.0, -100.0)
                    }
                    else -> {
                        weightContext.line(-100.0)
                    }
                }
            }
            .reduce { acc, vector -> acc.plus(vector) }
    }

}

/**
 * @return a multi-day [Vector] that weights down slots that conflict with the requestTimeZone
 * assuming a reasonable 8:00 - 20:00 "working hours" of the recipient
 */
data class RecipientTimeZoneWeigher(
    private val initiatorTimeZone: ZoneId,
    private val requestTimeZone: ZoneId
) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        val now = LocalDateTime.now()
        val userOffsetSeconds = initiatorTimeZone.rules.getOffset(now).totalSeconds
        val requestOffsetSeconds = requestTimeZone.rules.getOffset(now).totalSeconds
        if (userOffsetSeconds == requestOffsetSeconds) {
            return weightContext.multiDayZeroes()
        }

        val startTime = LocalTime.of(8, 0)
        val endTime = LocalTime.of(20, 0)

        val offsetMinutes = (userOffsetSeconds - requestOffsetSeconds) / 60
        val slotOffset = weightContext.minutesToSlotOffset(offsetMinutes)

        return (0..weightContext.days)
            .map { weightContext.linearCurve(startTime, endTime, -3.0, 0.0, -1 to 2) }
            .reduce { acc, vector -> acc.plus(vector) }
            .rotate(slotOffset)
    }
}

/**
 * @return a multi-day [Vector] that weights down slots that are further in the future
 */
class RecencyWeigher : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        val vector = curve(
            -1.0,
            0.5,
            weightContext.multiDayVectorSize,
            0,
            weightContext.singleDayVectorSize,
            0 to 0, { x -> x },
            -weightContext.multiDayVectorSize + weightContext.singleDayVectorSize to 0, { x -> x }
        )
        return vector
    }
}

/**
 * @return a multi-day [Vector] that weights down the suggested slot.
 */
data class SuggestedSlotWeigher(
    private val ss: SlotSuggestion,
    private val startDate: LocalDate
) : Weigher {
    override fun weigh(weightContext: WeightContext): Vector {
        val daySinceStart = startDate.until(ss.startDateTime.toLocalDate()).days

        val startTime = ss.startDateTime.toLocalTime()
        val endTime = ss.endDateTime.toLocalTime()

        return (0 until weightContext.days)
            .map {
                if (it == daySinceStart) {
                    weightContext.linearCurve(startTime, endTime, 0.0, -10.0, -4 to 0)
                } else {
                    weightContext.zeroes()
                }
            }
            .reduce { acc, vector -> acc.plus(vector) }
    }
}