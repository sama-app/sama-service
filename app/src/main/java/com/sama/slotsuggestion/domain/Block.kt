package com.sama.slotsuggestion.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.asSequence

data class Block(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val hasRecipients: Boolean,
    val recurrenceCount: Int = 1
) {

    /**
     * @return a copy of this [Block] with start and end times adjusted to the provided
     * timeZone
     */
    fun atTimeZone(timeZone: ZoneId): Block {
        return copy(
            startDateTime = startDateTime.withZoneSameInstant(timeZone),
            endDateTime = endDateTime.withZoneSameInstant(timeZone)
        )
    }

    /**
     * @return true if this [Block] spans multiple days
     */
    fun multiDay() = !startDateTime.toLocalDate().isEqual(endDateTime.toLocalDate())

    fun zeroDuration() = Duration.between(startDateTime, endDateTime) == Duration.ZERO

    fun isRecurring() = recurrenceCount > 1

    /**
     * Splits this [Block] into multiple [Block]s such that each block is within a single [java.time.LocalDate]. If
     * the [Block] is not [Block.multiDay] then a list containing itself is returned
     */
    fun splitByDate(): Collection<Block> {
        if (!multiDay()) {
            return listOf(this)
        }
        val zoneId = startDateTime.zone
        val startDate = startDateTime.toLocalDate()
        val endDate = endDateTime.toLocalDate()

        return startDate.datesUntil(endDate).asSequence()
            .map {
                when {
                    it.equals(startDate) -> copy(endDateTime = LocalTime.MAX.atDate(it).atZone(zoneId))
                    it.equals(endDate) -> copy(startDateTime = LocalTime.MIN.atDate(it).atZone(zoneId))
                    else -> copy(
                        startDateTime = LocalTime.MIN.atDate(it).atZone(zoneId),
                        endDateTime = LocalTime.MAX.atDate(it).atZone(zoneId)
                    )
                }
            }
            .toList()
    }

    fun toDebugString(): String {
        var string = ""

        val timeZone = startDateTime.zone
        val startDate = startDateTime.toLocalDate()
        val endDate = startDateTime.toLocalDate()
        if (startDate.isEqual(endDate)) {
            string += "$startDate ${startDateTime.toLocalTime()}-${endDateTime.toLocalTime()}"
        } else {
            string += "$startDate ${startDateTime.toLocalTime()} - $endDate ${endDateTime.toLocalTime()}"
        }
        string += " ($timeZone)"

        if (hasRecipients) {
            string += " with recipients"
        } else {
            string += " no recipients"
        }

        if (recurrenceCount > 1) {
            string += " recurring"
        }

        return string
    }
}

data class RecurrenceRule(
    val recurrence: Recurrence,
    /**
     * Repetition interval. For example, [Recurrence.DAILY] with interval 2 means
     * the [Block] repeats every other day
     */
    val interval: Int,
)

enum class Recurrence {
    YEARLY,
    MONTHLY,
    WEEKLY,
    DAILY,
}