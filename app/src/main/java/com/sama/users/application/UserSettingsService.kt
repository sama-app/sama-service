package com.sama.users.application

import com.sama.users.domain.UserId

interface UserSettingsService {
    fun find(userId: UserId): UserSettingsDTO
    fun create(userId: UserId): Boolean
    fun updateWorkingHours(userId: UserId, command: UpdateWorkingHoursCommand): Boolean
    fun updateTimeZone(userId: UserId, command: UpdateTimeZoneCommand): Boolean
    fun updateMarketingPreferences(userId: UserId, command: UpdateMarketingPreferencesCommand): Boolean
    fun grantPermissions(userId: UserId, command: GrantUserPermissionsCommand): Boolean
    fun revokePermissions(userId: UserId, command: RevokeUserPermissionsCommand): Boolean
}