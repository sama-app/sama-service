package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.*
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.*

@ApplicationService
@Service
class UserApplicationService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userSettingsDefaultsRepository: UserSettingsDefaultsRepository,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock
) {

    fun registerUser(command: RegisterUserCommand): Long {
        val userExistsByEmail = userRepository.existsByEmail(command.email)
        val userId = userRepository.nextIdentity()

        val userRegistration = UserRegistration(userId, command.email, userExistsByEmail, command.googleCredential)

        UserEntity.new(userRegistration).also { userRepository.save(it) }
        return userId
    }

    fun refreshCredentials(command: RefreshCredentialsCommand): Long {
        val user = userRepository.findByEmailOrThrow(command.email)

        user.applyChanges(command.googleCredential).also { userRepository.save(it) }
        return user.id()!!
    }

    fun registerDevice(userId: Long, command: RegisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrThrow(userId)

        val changes = UserDeviceRegistrations.of(user)
            .register(command.deviceId, command.firebaseRegistrationToken)
            .getOrThrow()

        user.applyChanges(changes).also { userRepository.save(it) }
        return true
    }

    fun unregisterDevice(userId: Long, command: UnregisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrThrow(userId)

        val changes = UserDeviceRegistrations.of(user)
            .unregister(command.deviceId)
            .getOrThrow()

        user.applyChanges(changes).also { userRepository.save(it) }
        return true
    }

    fun issueTokens(userId: Long): JwtPairDTO {
        val user = userRepository.findByIdOrThrow(userId)

        val userJwtIssuer = UserJwtIssuer.of(user)
        val refreshJwt = userJwtIssuer.issue(UUID.randomUUID(), refreshJwtConfiguration, clock)
            .getOrThrow()
        val accessJwt = userJwtIssuer.issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessJwt.token, refreshJwt.token)
    }

    fun refreshToken(command: RefreshTokenCommand): JwtPairDTO {
        val refreshToken = Jwt.verified(command.refreshToken, refreshJwtConfiguration)
            .getOrThrow()

        val user = userRepository.findByEmailOrThrow(refreshToken.userEmail())

        val accessToken = UserJwtIssuer.of(user)
            .issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessToken.token, refreshToken.token)
    }

    fun createUserSettings(userId: Long): Boolean {
        val userSettingsDefaults = userSettingsDefaultsRepository.findByIdOrNull(userId)

        val userSettings = UserSettings.createWithDefaults(userId, userSettingsDefaults)

        UserSettingsEntity.new(userSettings).also { userSettingsRepository.save(it) }
        return true
    }

    fun getUserSettings(userId: Long): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        return userSettings.let {
            UserSettingsDTO(
                it.locale!!,
                it.timezone!!,
                it.format24HourTime!!,
                it.workingHours().map { wh ->
                    DayWorkingHoursDTO(wh.key, wh.value.startTime, wh.value.endTime)
                }
            )
        }
    }

    fun updateWorkingHours(userId: Long, command: UpdateWorkingHoursCommand): Boolean {
        val entity = userSettingsRepository.findByIdOrThrow(userId)

        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }
        val userSettings = UserSettings.of(entity)
            .updateWorkingHours(workingHours)

        entity.applyChanges(userSettings).also { userSettingsRepository.save(it) }
        return true
    }
}