package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.meeting.application.InitiatorDTO
import com.sama.meeting.application.toInitiatorDTO
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    fun findPublicDetails(userId: UserId): UserDTO {
        val userEntity = userRepository.findByIdOrThrow(userId)
        return userEntity.let {
            UserDTO(it.publicId!!, it.fullName, it.email)
        }
    }

    @Transactional
    fun registerUser(command: RegisterUserCommand): UserId {
        val userExistsByEmail = userRepository.existsByEmail(command.email)

        val userRegistration =
            UserRegistration(command.email, userExistsByEmail, command.fullName, command.googleCredential)

        val userEntity = UserEntity.new(userRegistration).also { userRepository.save(it) }
        return userEntity.id!!
    }

    @Transactional
    fun deleteUser(userId: UserId): Boolean {
        userSettingsRepository.deleteById(userId)
        userRepository.deleteById(userId)
        return true
    }

    @Transactional
    fun refreshCredentials(command: RefreshCredentialsCommand): UserId {
        val user = userRepository.findByEmailOrThrow(command.email)

        user.applyChanges(command.googleCredential).also { userRepository.save(it) }
        return user.id!!
    }

    @Transactional
    fun updatePublicDetails(userId: UserId, command: UpdateUserPublicDetailsCommand): Boolean {
        val userEntity = userRepository.findByIdOrThrow(userId)

        val publicDetails = UserPublicDetails.of(userEntity)
            .rename(command.fullName)

        userEntity.applyChanges(publicDetails).also { userRepository.save(it) }
        return true
    }


    @Transactional
    fun findUserDeviceRegistrations(userId: UserId): UserDeviceRegistrationsDTO {
        val userEntity = userRepository.findByIdOrThrow(userId)
        return UserDeviceRegistrations.of(userEntity)
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
        val user = userRepository.findByIdOrThrow(userId)

        val changes = UserDeviceRegistrations.of(user)
            .register(command.deviceId, command.firebaseRegistrationToken)
            .getOrThrow()

        user.applyChanges(changes).also { userRepository.save(it) }
        return true
    }

    @Transactional
    fun unregisterDevice(userId: UserId, command: UnregisterDeviceCommand): Boolean {
        val user = userRepository.findByIdOrThrow(userId)

        val changes = UserDeviceRegistrations.of(user)
            .unregister(command.deviceId)
            .getOrThrow()

        user.applyChanges(changes).also { userRepository.save(it) }
        return true
    }

    fun issueTokens(userId: UserId): JwtPairDTO {
        val user = userRepository.findByIdOrThrow(userId)

        val userJwtIssuer = UserJwtIssuer.of(user)
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

        val user = if (refreshToken.userId() != null) {
            userRepository.findByPublicIdOrThrow(refreshToken.userId()!!)
        } else {
            userRepository.findByEmailOrThrow(refreshToken.userEmail())
        }

        val accessToken = UserJwtIssuer.of(user)
            .issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessToken.token, refreshToken.token)
    }

    @Transactional
    fun createUserSettings(userId: UserId): Boolean {
        val userSettingsDefaults = userSettingsDefaultsRepository.findByIdOrNull(userId)

        val userSettings = UserSettings.createWithDefaults(userId, userSettingsDefaults)

        UserSettingsEntity.new(userSettings).also { userSettingsRepository.save(it) }
        return true
    }

    @Transactional(readOnly = true)
    fun findUserSettings(userId: UserId): UserSettingsDTO {
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

    @Transactional
    fun updateWorkingHours(userId: UserId, command: UpdateWorkingHoursCommand): Boolean {
        val entity = userSettingsRepository.findByIdOrThrow(userId)

        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }
        val userSettings = UserSettings.of(entity)
            .updateWorkingHours(workingHours)

        entity.applyChanges(userSettings).also { userSettingsRepository.save(it) }
        return true
    }
}