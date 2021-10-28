package com.sama.users.infrastructure

import com.sama.api.config.security.UserPrincipal
import com.sama.users.application.AuthUserService
import com.sama.users.application.UserInternalDTO
import com.sama.users.application.toDTO
import com.sama.users.application.toInternalDTO
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettingsRepository
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SpringAuthUserService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository
) : AuthUserService {
    override fun currentUserIdOrNull(): UserId? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as UserPrincipal?
        val userId = principal?.userId
        return userId?.let { userRepository.findIdByPublicIdOrThrow(it) }
    }

    override fun currentUserId() = currentUserIdOrNull()
        ?: throw AuthenticationCredentialsNotFoundException("Missing authentication credentials")

    override fun currentUserOrNull(): UserInternalDTO? {
        val principal = SecurityContextHolder.getContext().authentication?.principal as UserPrincipal?
        val userId = principal?.userId ?: return null

        val user = userRepository.findByPublicIdOrThrow(userId)
        val userSettings = userSettingsRepository.findByIdOrThrow(user.id!!)
            .toDTO()
        return user.toInternalDTO(userSettings)
    }

    override fun currentUser() = currentUserOrNull()
        ?: throw AuthenticationCredentialsNotFoundException("Missing authentication credentials")
}