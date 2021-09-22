package com.sama.comms.infrastructure

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.CommsUserRepository
import com.sama.users.application.InternalUserService
import com.sama.users.application.UserService
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class ServiceCommsUserRepository(
    private val userService: UserService,
    private val userSettingsService: UserSettingsService,
) : CommsUserRepository {

    override fun find(userId: UserId): CommsUser {
        val user = userService.find(userId)
        val userSettings = userSettingsService.find(userId)
        return CommsUser(userId, userSettings.timeZone, user.email, user.fullName)
    }
}