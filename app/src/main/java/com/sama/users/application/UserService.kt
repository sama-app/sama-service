package com.sama.users.application

import com.sama.users.domain.UserId

interface UserService {
    fun find(userId: UserId): UserPublicDTO
    fun findAll(userIds: Collection<UserId>): Map<UserId, UserPublicDTO>
    fun findUserSettings(userId: UserId): UserSettingsDTO
    fun findUserDeviceRegistrations(userId: UserId): UserDeviceRegistrationsDTO
}