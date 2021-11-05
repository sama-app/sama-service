package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.ValueObject
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import javax.persistence.Embeddable

@DomainEntity
data class UserSettings(
    val userId: UserId,
    val locale: Locale,
    val timeZone: ZoneId,
    val format24HourTime: Boolean,
    val dayWorkingHours: Map<DayOfWeek, WorkingHours>,
    val meetingPreferences: MeetingPreferences,
    val newsletterSubscriptionEnabled: Boolean,
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
                MeetingPreferences.default(),
                newsletterSubscriptionEnabled = false,
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

    fun updateMeetingPreferences(update: MeetingPreferences): UserSettings {
        return copy(meetingPreferences = update)
    }

    fun enableNewsletterSubscription(): UserSettings {
        return copy(newsletterSubscriptionEnabled = true)
    }

    fun disableNewsletterSubscription(): UserSettings {
        return copy(newsletterSubscriptionEnabled = false)
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

@ValueObject
@Embeddable
data class MeetingPreferences(
    val defaultTitle: String?,
    val blockOutSlots: Boolean
) {
    companion object {
        fun default() = MeetingPreferences(
            defaultTitle = null,
            blockOutSlots = true
        )
    }
}

enum class UserPermission {
    PAST_EVENT_CONTACT_SCAN
}
