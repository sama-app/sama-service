package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.InvalidRefreshTokenException
import com.sama.users.domain.Jwt
import com.sama.users.domain.UserGoogleCredential
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRegistration
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsDefaultsRepository
import com.sama.users.domain.UserSettingsRepository
import com.sama.users.domain.WorkingHours
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    @Transactional(readOnly = true)
    fun findPublicDetails(userId: UserId): UserDTO {
        val userDetails = userRepository.findByIdOrThrow(userId)
        return userDetails.let {
            UserDTO(it.publicId!!, it.fullName, it.email)
        }
    }

    @Transactional
    fun registerUser(command: RegisterUserCommand): UserId {
        val userExistsByEmail = userRepository.existsByEmail(command.email)

        val userDetails = UserRegistration(command.email, userExistsByEmail, command.fullName)
            .validate()
            .let { userRepository.save(it) }

        userRepository.save(UserGoogleCredential(userDetails.id!!, command.googleCredential))

        return userDetails.id
    }

    @Transactional
    fun refreshCredentials(command: RefreshCredentialsCommand): UserId {
        val userDetails = userRepository.findByEmailOrThrow(command.email)
        userRepository.save(UserGoogleCredential(userDetails.id!!, command.googleCredential))
        return userDetails.id
    }

    @Transactional
    fun updatePublicDetails(userId: UserId, command: UpdateUserPublicDetailsCommand): Boolean {
        val userDetails = userRepository.findByIdOrThrow(userId)
            .rename(command.fullName)
        userRepository.save(userDetails)
        return true
    }

    @Transactional(readOnly = true)
    fun findUserDeviceRegistrations(userId: UserId): UserDeviceRegistrationsDTO {
        return userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .let {
                UserDeviceRegistrationsDTO(
                    if (it.deviceId != null && it.firebaseRegistrationToken != null) {
                        FirebaseDeviceRegistrationDTO(it.deviceId, it.firebaseRegistrationToken)
                    } else {
                        null
                    }
                )
            }
    }

    @Transactional
    fun registerDevice(userId: UserId, command: RegisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .register(command.deviceId, command.firebaseRegistrationToken)
            .getOrThrow()
        userRepository.save(changes)
        return true
    }

    @Transactional
    fun unregisterDevice(userId: UserId, command: UnregisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .unregister(command.deviceId)
            .getOrThrow()

        userRepository.save(changes)
        return true
    }

    fun issueTokens(userId: UserId): JwtPairDTO {
        val userJwtIssuer = userRepository.findJwtIssuerByIdOrThrow(userId)
        val refreshJwt = userJwtIssuer.issue(UUID.randomUUID(), refreshJwtConfiguration, clock)
            .getOrThrow()
        val accessJwt = userJwtIssuer.issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessJwt.token, refreshJwt.token)
    }

    fun refreshToken(command: RefreshTokenCommand): JwtPairDTO {
        val refreshToken = Jwt.verified(command.refreshToken, refreshJwtConfiguration, clock)
            .onFailure { throw InvalidRefreshTokenException() }
            .getOrThrow()

        val userJwtIssuer = if (refreshToken.userId() != null) {
            userRepository.findJwtIssuerByPublicOrThrow(refreshToken.userId()!!)
        } else {
            userRepository.findJwtIssuerByEmailOrThrow(refreshToken.userEmail())
        }

        val accessToken = userJwtIssuer
            .issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessToken.token, refreshToken.token)
    }

    @Transactional
    fun createUserSettings(userId: UserId): Boolean {
        val userSettingsDefaults = userSettingsDefaultsRepository.findByIdOrNull(userId)
        val userSettings = UserSettings.createWithDefaults(userId, userSettingsDefaults)
        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional(readOnly = true)
    fun findUserSettings(userId: UserId): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        return userSettings.let {
            UserSettingsDTO(
                it.locale,
                it.timezone,
                it.format24HourTime,
                it.dayWorkingHours.map { wh ->
                    DayWorkingHoursDTO(wh.key, wh.value.startTime, wh.value.endTime)
                }
            )
        }
    }

    @Transactional
    fun updateWorkingHours(userId: UserId, command: UpdateWorkingHoursCommand): Boolean {
        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }

        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .updateWorkingHours(workingHours)

        userSettingsRepository.save(userSettings)
        return true
    }
}