package com.sama.users.application

import com.sama.common.NotFoundException
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.Jwt
import com.sama.users.domain.JwtPair
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettingsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@Service
class UserApplicationService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock
) {

    fun registerDevice(userId: Long, command: RegisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        user.registerFirebaseDevice(command.deviceId, command.firebaseRegistrationToken)
        userRepository.save(user)
        return true
    }

    fun unregisterDevice(userId: Long, command: UnregisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        user.unregisterFirebaseDevice(command.deviceId)
        userRepository.save(user)
        return true
    }

    fun refreshToken(command: RefreshTokenCommand): JwtPair {
        val email = Jwt(command.refreshToken, refreshJwtConfiguration).userEmail()
        val user = userRepository.findByEmail(email)
            ?: throw NotFoundException(0L)
        return user.refreshJwt(command.refreshToken, accessJwtConfiguration, refreshJwtConfiguration, clock)
    }

    fun getSettings(userId: Long): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrNull(userId)
        return userSettings?.let {
            UserSettingsDTO(
                it.locale, it.timezone, it.format24HourTime,
                it.workingHours.map { wh ->
                    DayWorkingHoursDTO(wh.value.dayOfWeek, wh.value.startTime, wh.value.endTime)
                }
            )
        }
            ?: throw NotFoundException(userId)
    }
}

data class RegisterDeviceCommand(val deviceId: UUID, val firebaseRegistrationToken: String)
data class UnregisterDeviceCommand(val deviceId: UUID)
data class RefreshTokenCommand(val refreshToken: String)

data class UserSettingsDTO(
    val locale: Locale,
    val timezone: ZoneId,
    val format24HourTime: Boolean,
    val workingHours: List<DayWorkingHoursDTO>,
)

data class DayWorkingHoursDTO(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
)
