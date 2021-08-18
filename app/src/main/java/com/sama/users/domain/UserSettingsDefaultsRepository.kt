package com.sama.users.domain

import com.sama.common.DomainRepository
import com.sama.common.NotFoundException
import org.springframework.data.repository.Repository

@DomainRepository
interface UserSettingsDefaultsRepository : Repository<UserSettingsDefaults, UserId> {
    fun findByIdOrNull(userId: Long): UserSettingsDefaults?
    fun findByIdOrThrow(userId: Long) = findByIdOrNull(userId)
        ?: throw NotFoundException(UserSettingsDefaults::class, userId)
}