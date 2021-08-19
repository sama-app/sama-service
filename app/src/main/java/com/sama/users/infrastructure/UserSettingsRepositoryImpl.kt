package com.sama.users.infrastructure

import com.sama.common.findByIdOrThrow
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsRepository
import com.sama.users.infrastructure.jpa.UserSettingsEntity
import com.sama.users.infrastructure.jpa.UserSettingsJpaRepository
import org.springframework.stereotype.Component

@Component
class UserSettingsRepositoryImpl(private val userSettingsJpaRepository: UserSettingsJpaRepository) :
    UserSettingsRepository {
    override fun findByIdOrThrow(userId: UserId): UserSettings {
        return userSettingsJpaRepository.findByIdOrThrow(userId.id).toDomainObject()
    }

    override fun save(userSettings: UserSettings): UserSettings {
        var userSettingsEntity = UserSettingsEntity.new(userSettings)
        userSettingsEntity = userSettingsJpaRepository.save(userSettingsEntity)
        return userSettingsEntity.toDomainObject()
    }
}

fun UserSettingsEntity.toDomainObject(): UserSettings {
    return UserSettings(
        userId.toUserId(),
        locale!!,
        timezone!!,
        format24HourTime!!,
        dayWorkingHours.mapValues { it.value.workingHours }
    )
}