package com.sama.users.application

import com.sama.users.domain.UserPermission
import java.time.ZoneId
import java.util.UUID

data class RegisterUserCommand(val email: String, val fullName: String?)
data class RegisterDeviceCommand(val deviceId: UUID, val firebaseRegistrationToken: String)
data class UnregisterDeviceCommand(val deviceId: UUID)
data class RefreshTokenCommand(val refreshToken: String)
data class UpdateMarketingPreferencesCommand(val newsletterSubscriptionEnabled: Boolean)
data class UpdateWorkingHoursCommand(val workingHours: List<DayWorkingHoursDTO>)
data class UpdateTimeZoneCommand(val timeZone: ZoneId)
data class UpdateUserPublicDetailsCommand(val fullName: String?)
data class GrantUserPermissionsCommand(val permissions: Set<UserPermission>)
data class RevokeUserPermissionsCommand(val permissions: Set<UserPermission>)