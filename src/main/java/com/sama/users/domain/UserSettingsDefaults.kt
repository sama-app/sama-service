package com.sama.users.domain

import java.time.DayOfWeek
import java.time.ZoneId
import java.util.*

data class UserSettingsDefaults(
    val locale: Locale?,
    val timezone: ZoneId?,
    val format24HourTime: Boolean?,
    val workingHours: Map<DayOfWeek, WorkingHours>?
)