package com.sama.users.domain

import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

data class UserSettingsDefaults(
    val locale: Locale? = null,
    val timezone: ZoneId? = null,
    val format24HourTime: Boolean? = null,
    val workingHours: Map<DayOfWeek, WorkingHours>? = null
)