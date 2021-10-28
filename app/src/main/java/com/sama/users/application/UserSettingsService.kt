package com.sama.users.application

import com.sama.users.domain.UserId

interface UserSettingsService {
    fun me(): UserSettingsDTO
    fun find(userId: UserId): UserSettingsDTO
    fun create(userId: UserId): Boolean
    fun updateWorkingHours(command: UpdateWorkingHoursCommand): Boolean
    fun updateTimeZone(command: UpdateTimeZoneCommand): Boolean
    fun updateMarketingPreferences(command: UpdateMarketingPreferencesCommand): Boolean
    fun grantPermissions(command: GrantUserPermissionsCommand): Boolean
    fun revokePermissions(command: RevokeUserPermissionsCommand): Boolean
}