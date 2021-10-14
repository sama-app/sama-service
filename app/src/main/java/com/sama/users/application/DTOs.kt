package com.sama.users.application

import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPermission
import com.sama.users.domain.UserPublicId
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.TIME

data class UserPublicDTO(
    val userId: UserPublicId,
    val fullName: String?,
    val email: String
)

data class UserInternalDTO(
    var id: UserId,
    val publicId: UserPublicId,
    val fullName: String?,
    val email: String,
    val settings: UserSettingsDTO
)

fun UserDetails.toInternalDTO(userSettings: UserSettingsDTO) =
    UserInternalDTO(id!!, publicId!!, fullName, email, userSettings)

data class UserDeviceRegistrationsDTO(
    val firebaseDeviceRegistrations: List<FirebaseDeviceRegistrationDTO>
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
    val timeZone: ZoneId,
    val format24HourTime: Boolean,
    val workingHours: List<DayWorkingHoursDTO>,
    val grantedPermissions: Set<UserPermission>,
    val marketingPreferences: MarketingPreferencesDTO
)

data class MarketingPreferencesDTO(val newsletterSubscriptionEnabled: Boolean)

data class DayWorkingHoursDTO(
    val dayOfWeek: DayOfWeek,
    @DateTimeFormat(iso = TIME) val startTime: LocalTime,
    @DateTimeFormat(iso = TIME) val endTime: LocalTime
)

data class JwtPairDTO(
    val accessToken: String,
    val refreshToken: String
)