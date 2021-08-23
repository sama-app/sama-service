package com.sama.slotsuggestion.infrastructure

import com.sama.slotsuggestion.domain.User
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.WorkingHours
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class ServiceUserRepository(private val userSettingsService: UserSettingsService) : UserRepository {
    override fun findById(userId: UserId): User {
        val userSettings = userSettingsService.find(userId)
        return User(
            userId,
            userSettings.timeZone,
            userSettings.workingHours.associate {
                it.dayOfWeek to WorkingHours(it.startTime, it.endTime)
            })
    }
}