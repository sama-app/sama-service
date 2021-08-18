package com.sama.users.domain

import com.sama.common.DomainRepository
import org.springframework.data.repository.Repository

@DomainRepository
interface UserSettingsRepository : Repository<UserSettings, UserId> {
    fun findByIdOrThrow(userId: UserId): UserSettings
    fun save(userSettings: UserSettings): UserSettings
}