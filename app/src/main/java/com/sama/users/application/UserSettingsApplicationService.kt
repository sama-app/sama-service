package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsDefaultsRepository
import com.sama.users.domain.UserSettingsRepository
import com.sama.users.domain.WorkingHours
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserSettingsApplicationService(
    private val userSettingsRepository: UserSettingsRepository,
    private val userSettingsDefaultsRepository: UserSettingsDefaultsRepository,
) : UserSettingsService {

    @Transactional
    fun createUserSettings(userId: UserId): Boolean {
        val userSettingsDefaults = userSettingsDefaultsRepository.findById(userId)
        val userSettings = UserSettings.createWithDefaults(userId, userSettingsDefaults)
        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional(readOnly = true)
    override fun find(userId: UserId): UserSettingsDTO {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
        return userSettings.let {
            UserSettingsDTO(
                it.locale,
                it.timeZone,
                it.format24HourTime,
                it.dayWorkingHours
                    .map { wh -> DayWorkingHoursDTO(wh.key, wh.value.startTime, wh.value.endTime) }
                    .sortedBy { wh -> wh.dayOfWeek }
            )
        }
    }

    @Transactional
    fun updateWorkingHours(userId: UserId, command: UpdateWorkingHoursCommand): Boolean {
        val workingHours = command.workingHours
            .associate { Pair(it.dayOfWeek, WorkingHours(it.startTime, it.endTime)) }

        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .updateWorkingHours(workingHours)

        userSettingsRepository.save(userSettings)
        return true
    }

    @Transactional
    fun updateTimeZone(userId: UserId, command: UpdateTimeZoneCommand): Boolean {
        val userSettings = userSettingsRepository.findByIdOrThrow(userId)
            .updateTimeZone(command.timeZone)

        userSettingsRepository.save(userSettings)
        return true
    }
}