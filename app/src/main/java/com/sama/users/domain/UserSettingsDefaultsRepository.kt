package com.sama.users.domain

import com.sama.common.DomainRepository
import com.sama.common.NotFoundException
import org.springframework.data.repository.Repository

@DomainRepository
interface UserSettingsDefaultsRepository : Repository<UserSettingsDefaults, UserId> {
    fun findByIdOrNull(userId: UserId): UserSettingsDefaults?
    fun findByIdOrThrow(userId: UserId) = findByIdOrNull(userId)
        ?: throw NotFoundException(UserSettingsDefaults::class, userId)
}