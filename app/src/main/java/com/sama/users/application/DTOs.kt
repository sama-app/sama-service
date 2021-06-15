package com.sama.users.application

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.TIME
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

data class GoogleOauth2Redirect(
    val authorizationUrl: String
)

data class UserSettingsDTO(
    val locale: Locale,
    val timezone: ZoneId,
    val format24HourTime: Boolean,
    val workingHours: List<DayWorkingHoursDTO>,
)

data class DayWorkingHoursDTO(
    val dayOfWeek: DayOfWeek,
    @DateTimeFormat(iso = TIME) val startTime: LocalTime,
    @DateTimeFormat(iso = TIME) val endTime: LocalTime
)

data class JwtPairDTO(
    val accessToken: String,
    val refreshToken: String
)