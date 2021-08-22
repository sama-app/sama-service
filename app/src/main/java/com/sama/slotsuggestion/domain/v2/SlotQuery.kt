package com.sama.slotsuggestion.domain.v2

import java.time.*
import java.time.DayOfWeek.*
import java.util.function.Predicate


private val WORKDAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
private val WEEKEND = setOf(SATURDAY, SUNDAY)

inline fun slotQuery(query: SlotQuery.() -> Unit): SlotQuery {
    val slotQuery = SlotQuery()
    query.invoke(slotQuery)
    return slotQuery
}

data class SlotQuery(
    private var fromDateTime: LocalDateTime = LocalDateTime.MIN,
    private var toDateTime: LocalDateTime = LocalDateTime.MAX,
    private var fromTime: LocalTime = LocalTime.MIDNIGHT,
    private var toTime: LocalTime? = null, // null indicates MIDNIGHT of the next day
    private var fromDate: LocalDate = LocalDate.MIN,
    private var toDate: LocalDate = LocalDate.MAX,
    private var daysOfWeek: Set<DayOfWeek>? = null,
) : Predicate<Slot> {

    fun from(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        fromDateTime = zonedDateTime.withZoneSameInstant(zoneId).toLocalDateTime()
    }

    fun to(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        toDateTime = zonedDateTime.withZoneSameInstant(zoneId).toLocalDateTime()
    }

    fun fromDate(localDate: LocalDate) {
        fromDate = localDate
    }

    fun fromDate(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        fromDate(zonedDateTime.withZoneSameInstant(zoneId).toLocalDate())
    }

    fun toDate(localDate: LocalDate) {
        toDate = localDate
    }

    fun toDate(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        toDate(zonedDateTime.withZoneSameInstant(zoneId).toLocalDate())
    }

    fun inTimeRange(startTime: LocalTime, endTime: LocalTime) {
        this.fromTime = startTime
        this.toTime = endTime
    }

    fun toMidnight(fromTime: LocalTime) {
        this.fromTime = fromTime
        this.toTime = null
    }

    fun fromMidnight(toTime: LocalTime) {
        this.fromTime = LocalTime.MIN
        this.toTime = toTime
    }

    fun dayOfWeek(dayOfWeek: DayOfWeek) {
        this.daysOfWeek = setOf(dayOfWeek)
    }

    fun workdays() {
        this.daysOfWeek = WORKDAYS
    }

    fun weekend() {
        this.daysOfWeek = WEEKEND
    }

    override fun test(slot: Slot): Boolean {
        if (slot.startDateTime.isBefore(fromDateTime)) {
            return false
        }
        if (slot.endDateTime.isAfter(toDateTime)) {
            return false
        }

        val startLocalDate = slot.startDateTime.toLocalDate()
        val endLocalDate = slot.endDateTime.toLocalDate()
        if (startLocalDate.isBefore(fromDate)) {
            return false
        }
        if (startLocalDate.isAfter(toDate)) {
            return false
        }
        val startLocalTime = slot.startDateTime.toLocalTime()
        val endLocalTime = slot.endDateTime.toLocalTime()
        if (fromTime.isBefore(toTime ?: LocalTime.MAX)) {
            if (startLocalTime.isBefore(fromTime) ||
                (toTime != null && endLocalTime.isAfter(toTime)) ||
                (toTime != null && !startLocalDate.isEqual(endLocalDate))
            ) {
                return false
            }
        } else {
            check(toTime != null)
            if (startLocalTime.isAfter(toTime) && startLocalTime.isBefore(fromTime)) {
                return false
            }
        }
        if (daysOfWeek != null && slot.dayOfWeek !in daysOfWeek!!) {
            return false
        }
        return true
    }
}

fun DayOfWeek.isWorkday(): Boolean {
    return this !in WEEKEND
}