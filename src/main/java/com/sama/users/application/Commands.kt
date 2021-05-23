package com.sama.users.application

import com.sama.users.domain.GoogleCredential
import java.util.*

data class RegisterUserCommand(val email: String, val googleCredential: GoogleCredential)
data class RefreshCredentialsCommand(val email: String, val googleCredential: GoogleCredential)
data class RegisterDeviceCommand(val deviceId: UUID, val firebaseRegistrationToken: String)
data class UnregisterDeviceCommand(val deviceId: UUID)
data class RefreshTokenCommand(val refreshToken: String)
data class UpdateWorkingHoursCommand(val workingHours: List<DayWorkingHoursDTO>)