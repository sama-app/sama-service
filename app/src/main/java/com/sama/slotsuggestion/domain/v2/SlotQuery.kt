package com.sama.slotsuggestion.domain.v2

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.function.Predicate


private val WORKDAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
private val WEEKEND = setOf(SATURDAY, SUNDAY)

inline fun slotQuery(query: SlotQuery.() -> Unit): SlotQuery {
    val slotQuery = SlotQuery()
    query.invoke(slotQuery)
    return slotQuery
}

data class SlotQuery(
    var fromDateTime: LocalDateTime = LocalDateTime.MIN,
    var toDateTime: LocalDateTime = LocalDateTime.MAX,
    var fromTime: LocalTime = LocalTime.MIDNIGHT,
    var toTime: LocalTime? = null, // null indicates MIDNIGHT of the next day
    var fromDate: LocalDate = LocalDate.MIN,
    var toDate: LocalDate = LocalDate.MAX,
    var daysOfWeek: Set<DayOfWeek>? = null,
) : Predicate<Slot> {

    fun from(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        fromDateTime = zonedDateTime.withZoneSameInstant(zoneId).toLocalDateTime()
    }

    fun to(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        toDateTime = zonedDateTime.withZoneSameInstant(zoneId).toLocalDateTime()
    }

    fun fromDate(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        fromDate = zonedDateTime.withZoneSameInstant(zoneId).toLocalDate()
    }

    fun toDate(zonedDateTime: ZonedDateTime, zoneId: ZoneId) {
        toDate = zonedDateTime.withZoneSameInstant(zoneId).toLocalDate()
    }

    fun timeRange(startTime: LocalTime, endTime: LocalTime) {
        this.fromTime = startTime
        this.toTime = endTime
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
            if ((toTime != null && startLocalTime.isBefore(toTime)) || endLocalTime.isAfter(fromTime)) {
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