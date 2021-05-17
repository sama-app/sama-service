package com.sama.users.application

import com.sama.common.NotFoundException
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.*
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
            ?: throw NotFoundException(User::class, userId)
        user.registerFirebaseDevice(command.deviceId, command.firebaseRegistrationToken)
        userRepository.save(user)
        return true
    }

    fun unregisterDevice(userId: Long, command: UnregisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(User::class, userId)
        user.unregisterFirebaseDevice(command.deviceId)
        userRepository.save(user)
        return true
    }

    fun refreshToken(command: RefreshTokenCommand): JwtPair {
        val email = Jwt(command.refreshToken, refreshJwtConfiguration).userEmail()
        val user = userRepository.findByEmail(email)
            ?: throw NotFoundException(User::class, "email", email)
        return user.refreshJwt(command.refreshToken, accessJwtConfiguration, refreshJwtConfiguration, clock)
    }

    fun getSettings(userId: Long): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(User::class, userId)
        return userSettings.let {
            UserSettingsDTO(
                it.locale, it.timezone, it.format24HourTime,
                it.workingHours().map { wh ->
                    DayWorkingHoursDTO(wh.key, wh.value.startTime, wh.value.endTime)
                }
            )
        }
    }

    fun updateWorkingHours(userId: Long, command: UpdateWorkingHoursCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(User::class, userId)

        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }
        userSettings.updateWorkingHours(workingHours)

        userSettingsRepository.save(userSettings)
        return true
    }
}

data class RegisterDeviceCommand(val deviceId: UUID, val firebaseRegistrationToken: String)
data class UnregisterDeviceCommand(val deviceId: UUID)
data class RefreshTokenCommand(val refreshToken: String)
data class UpdateWorkingHoursCommand(val workingHours: List<DayWorkingHoursDTO>)

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
