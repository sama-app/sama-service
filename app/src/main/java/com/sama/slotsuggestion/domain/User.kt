package com.sama.slotsuggestion.domain

import com.sama.users.domain.UserId
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

data class User(
    val userId: UserId,
    val timeZone: ZoneId,
    val workingHours: Map<DayOfWeek, WorkingHours>
)

data class WorkingHours(val startTime: LocalTime, val endTime: LocalTime) {
    fun isAllDay(): Boolean {
        return startTime == LocalTime.MIN && endTime == LocalTime.MAX
    }
}