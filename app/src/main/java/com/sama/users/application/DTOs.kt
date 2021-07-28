package com.sama.users.application

import com.sama.users.domain.UserId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.TIME
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

data class UserDTO(
    val userId: UserId,
    val email: String,
    val fullName: String?,
    val active: Boolean
)

data class UserDeviceRegistrationsDTO(
    val userId: UserId,
    val firebaseDeviceRegistration: FirebaseDeviceRegistrationDTO?
)

data class FirebaseDeviceRegistrationDTO(
    val deviceId: UUID,
    val registrationToken: String
)

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