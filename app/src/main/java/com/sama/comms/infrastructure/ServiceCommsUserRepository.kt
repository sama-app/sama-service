package com.sama.comms.infrastructure

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.CommsUserRepository
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class ServiceCommsUserRepository(private val userService: UserService) : CommsUserRepository {
    override fun findById(userId: UserId): CommsUser {
        val user = userService.find(userId)
        val userSettings = userService.findUserSettings(userId)
        return CommsUser(userId, userSettings.timeZone, user.email)
    }
}