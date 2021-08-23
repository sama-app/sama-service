package com.sama.users.application

import com.sama.users.domain.UserId

interface UserSettingsService {
    fun find(userId: UserId): UserSettingsDTO
}