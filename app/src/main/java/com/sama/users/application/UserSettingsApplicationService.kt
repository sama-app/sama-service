package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.integration.mailerlite.MailerLiteClient
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsDefaultsRepository
import com.sama.users.domain.UserSettingsRepository
import com.sama.users.domain.WorkingHours
import java.time.Instant
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserSettingsApplicationService(
    private val userSettingsRepository: UserSettingsRepository,
    private val userSettingsDefaultsRepository: UserSettingsDefaultsRepository,
    private val userRepository: UserRepository,
    private val mailerLiteClient: MailerLiteClient,
    private val taskScheduler: TaskScheduler,
) : UserSettingsService {

    @Transactional
    override fun create(userId: UserId): Boolean {
        val userSettingsDefaults = userSettingsDefaultsRepository.findById(userId)
        val userSettings = UserSettings.createWithDefaults(userId, userSettingsDefaults)
        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional(readOnly = true)
    override fun find(userId: UserId): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        return userSettings.toDTO()
    }

    @Transactional
    override fun updateWorkingHours(userId: UserId, command: UpdateWorkingHoursCommand): Boolean {
        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }

        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .updateWorkingHours(workingHours)

        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional
    override fun updateTimeZone(userId: UserId, command: UpdateTimeZoneCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .updateTimeZone(command.timeZone)

        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional
    override fun updateMarketingPreferences(userId: UserId, command: UpdateMarketingPreferencesCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        val updated = userSettings
            .let {
                if (command.newsletterSubscriptionEnabled) {
                    it.enableNewsletterSubscription()
                } else {
                    it.disableNewsletterSubscription()
                }
            }

        if (userSettings != updated) {
            userSettingsRepository.save(updated)

            taskScheduler.schedule({
                val user = userRepository.findByIdOrThrow(userId)
                if (updated.newsletterSubscriptionEnabled) {
                    mailerLiteClient.addSubscriber(user.email, user.fullName)
                } else {
                    mailerLiteClient.removeSubscriber(user.email)
                }
            }, Instant.now())
        }
        return true
    }

    @Transactional
    override fun grantPermissions(userId: UserId, command: GrantUserPermissionsCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .grantPermissions(command.permissions)

        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional
    override fun revokePermissions(userId: UserId, command: RevokeUserPermissionsCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .revokePermissions(command.permissions)

        userSettingsRepository.save(userSettings)
        return true
    }
}

fun UserSettings.toDTO(): UserSettingsDTO {
    return UserSettingsDTO(
        locale,
        timeZone,
        format24HourTime,
        dayWorkingHours
            .map { wh -> DayWorkingHoursDTO(wh.key, wh.value.startTime, wh.value.endTime) }
            .sortedBy { wh -> wh.dayOfWeek },
        grantedPermissions,
        MarketingPreferencesDTO(newsletterSubscriptionEnabled)
    )
}