package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import com.sama.users.domain.UserRegistration
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserApplicationService(private val userRepository: UserRepository) :
    InternalUserService, UserService {

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
}