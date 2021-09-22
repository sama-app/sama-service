package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.ValueObject
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.persistence.*

@DomainEntity
data class UserSettings(
    val userId: UserId,
    val locale: Locale,
    val timeZone: ZoneId,
    val format24HourTime: Boolean,
    val dayWorkingHours: Map<DayOfWeek, WorkingHours>,
    val grantedPermissions: Set<UserPermission>,
) {

    @Factory
    companion object {
        fun createWithDefaults(userId: UserId, defaults: UserSettingsDefaults?): UserSettings {
            return UserSettings(
                userId,
                locale = defaults?.locale ?: Locale.ENGLISH,
                timeZone = defaults?.timezone ?: ZoneId.of("Etc/GMT"),
                format24HourTime = defaults?.format24HourTime ?: false,
                dayWorkingHours = defaults?.workingHours ?: listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                    .associateWith { WorkingHours.nineToFive() },
                grantedPermissions = emptySet()
            )
        }

    }

    fun updateWorkingHours(dayWorkingHours: Map<DayOfWeek, WorkingHours>): UserSettings {
        return copy(dayWorkingHours = dayWorkingHours)
    }

    fun updateTimeZone(timeZone: ZoneId): UserSettings {
        return copy(timeZone = timeZone)
    }

    fun grantPermissions(permissions: Set<UserPermission>): UserSettings {
        return copy(grantedPermissions = grantedPermissions + permissions)
    }

    fun revokePermissions(permissions: Set<UserPermission>): UserSettings {
        return copy(grantedPermissions = grantedPermissions - permissions)
    }
}

@ValueObject
@Embeddable
data class WorkingHours(
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    init {
        if (!startTime.isBefore(endTime)) {
            throw IllegalArgumentException("WorkingHours: startTime ($startTime) must be before endTime ($endTime)")
        }
    }

    companion object {
        fun nineToFive(): WorkingHours {
            return WorkingHours(
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0)
            )
        }
    }
}

enum class UserPermission {
    PAST_EVENT_CONTACT_SCAN
}
