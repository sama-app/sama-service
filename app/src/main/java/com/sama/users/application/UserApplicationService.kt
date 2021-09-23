package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.InvalidRefreshTokenException
import com.sama.users.domain.Jwt
import com.sama.users.domain.UserId
import com.sama.users.domain.UserJwtIssuer
import com.sama.users.domain.UserPublicId
import com.sama.users.domain.UserRegistration
import com.sama.users.domain.UserRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserApplicationService(
    private val userRepository: UserRepository,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock,
) : InternalUserService, UserService {

    @Transactional(readOnly = true)
    override fun find(userId: UserId): UserPublicDTO {
        val userDetails = userRepository.findByIdOrThrow(userId)
        return userDetails.let {
            UserPublicDTO(it.publicId!!, it.fullName, it.email)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(userIds: Collection<UserId>): Map<UserId, UserPublicDTO> {
        return userRepository.findByIds(userIds)
            .associate { it.id!! to UserPublicDTO(it.publicId!!, it.fullName, it.email) }
    }

    @Transactional
    fun registerUser(command: RegisterUserCommand): UserId {
        val userExistsByEmail = userRepository.existsByEmail(command.email)

        val userDetails = UserRegistration(command.email, userExistsByEmail, command.fullName)
            .validate()
            .let { userRepository.save(it) }

        return userDetails.id!!
    }

    @Transactional
    fun updatePublicDetails(userId: UserId, command: UpdateUserPublicDetailsCommand): Boolean {
        val userDetails = userRepository.findByIdOrThrow(userId)
            .rename(command.fullName)
        userRepository.save(userDetails)
        return true
    }

    @Transactional(readOnly = true)
    override fun findUserDeviceRegistrations(userId: UserId): UserDeviceRegistrationsDTO {
        return userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .deviceRegistrations
            .map { FirebaseDeviceRegistrationDTO(it.deviceId, it.firebaseRegistrationToken) }
            .let { UserDeviceRegistrationsDTO(it) }
    }

    override fun translatePublicId(userPublicId: UserPublicId): UserId {
        return userRepository.findIdByPublicIdOrThrow(userPublicId)
    }

    override fun findInternalByEmail(email: String): UserInternalDTO {
        return userRepository.findByEmailOrThrow(email)
            .let { UserInternalDTO(it.id!!, it.publicId!!, it.fullName, it.email) }
    }

    override fun findInternalByPublicId(userPublicId: UserPublicId): UserInternalDTO {
        return userRepository.findByPublicIdOrThrow(userPublicId)
            .let { UserInternalDTO(it.id!!, it.publicId!!, it.fullName, it.email) }
    }

    @Transactional
    fun registerDevice(userId: UserId, command: RegisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .register(command.deviceId, command.firebaseRegistrationToken)
        userRepository.save(changes)
        return true
    }

    @Transactional
    fun unregisterDevice(userId: UserId, command: UnregisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .unregister(command.deviceId)
        userRepository.save(changes)
        return true
    }

    fun issueTokens(userId: UserId): JwtPairDTO {
        val userDetails = userRepository.findByIdOrThrow(userId)
        val userJwtIssuer = UserJwtIssuer(userDetails)
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

        val userDetails = if (refreshToken.userId() != null) {
            userRepository.findByPublicIdOrThrow(refreshToken.userId()!!)
        } else {
            userRepository.findByEmailOrThrow(refreshToken.userEmail())
        }

        val accessToken = UserJwtIssuer(userDetails)
            .issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessToken.token, refreshToken.token)
    }
}