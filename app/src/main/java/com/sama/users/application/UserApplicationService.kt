package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import com.sama.users.domain.UserRegistration
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserApplicationService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val authUserService: AuthUserService
) : InternalUserService, UserService {

    @Transactional(readOnly = true)
    override fun me() = find(authUserService.currentUserId())

    @Transactional(readOnly = true)
    override fun find(userId: UserId) = userRepository.findByIdOrThrow(userId)
        .let { UserPublicDTO(it.publicId!!, it.fullName, it.email) }

    @Transactional(readOnly = true)
    override fun findAll(userIds: Collection<UserId>) = userRepository.findByIds(userIds)
        .associate { it.id!! to UserPublicDTO(it.publicId!!, it.fullName, it.email) }

    @Transactional
    override fun register(command: RegisterUserCommand): UserId {
        val userExistsByEmail = userRepository.existsByEmail(command.email)

        val userDetails = UserRegistration(command.email, userExistsByEmail, command.fullName)
            .validate()
            .let { userRepository.save(it) }

        return userDetails.id!!
    }

    @Transactional
    override fun updatePublicDetails(userId: UserId, command: UpdateUserPublicDetailsCommand): Boolean {
        val userDetails = userRepository.findByIdOrThrow(userId)
            .rename(command.fullName)
        userRepository.save(userDetails)
        return true
    }

    @Transactional(readOnly = true)
    override fun findInternal(userId: UserId): UserInternalDTO {
        val user = userRepository.findByIdOrThrow(userId)
        val userSettings = userSettingsRepository.findByIdOrThrow(user.id!!)
        return user.let { user.toInternalDTO(userSettings.toDTO()) }
    }

    override fun findInternalByEmail(email: String): UserInternalDTO {
        val user = userRepository.findByEmailOrThrow(email)
        val userSettings = userSettingsRepository.findByIdOrThrow(user.id!!)
        return user.let { user.toInternalDTO(userSettings.toDTO()) }
    }

    override fun findInternalByPublicId(userPublicId: UserPublicId): UserInternalDTO {
        val user = userRepository.findByPublicIdOrThrow(userPublicId)
        val userSettings = userSettingsRepository.findByIdOrThrow(user.id!!)
        return user.let { user.toInternalDTO(userSettings.toDTO()) }
    }

    override fun translatePublicId(userPublicId: UserPublicId) =
        userRepository.findIdByPublicIdOrThrow(userPublicId)

    override fun findIdsByEmail(emails: Set<String>) =
        userRepository.findIdsByEmail(emails)
}