package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.ValueObject
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters.ZoneIdConverter
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.persistence.*
import javax.persistence.CascadeType.ALL
import javax.persistence.FetchType.LAZY
import javax.persistence.GenerationType.IDENTITY

@DomainEntity
data class UserSettings(
    val userId: UserId,
    val locale: Locale,
    val timezone: ZoneId,
    val format24HourTime: Boolean,
    val dayWorkingHours: Map<DayOfWeek, WorkingHours>,
) {

    @Factory
    companion object {
        fun createWithDefaults(userId: UserId, defaults: UserSettingsDefaults?): UserSettings {
            return UserSettings(
                userId,
                locale = defaults?.locale ?: Locale.ENGLISH,
                timezone = defaults?.timezone ?: ZoneId.of("Etc/GMT"),
                format24HourTime = defaults?.format24HourTime ?: false,
                dayWorkingHours = defaults?.workingHours ?: listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                    .associateWith { WorkingHours.nineToFive() }
            )
        }

        fun of(userSettings: UserSettingsEntity): UserSettings {
            return UserSettings(
                userSettings.userId, userSettings.locale!!, userSettings.timezone!!,
                userSettings.format24HourTime!!, userSettings.workingHours()
            )
        }
    }

    fun updateWorkingHours(dayWorkingHours: Map<DayOfWeek, WorkingHours>): UserSettings {
        return copy(dayWorkingHours = dayWorkingHours)
    }
}

@ValueObject
@Embeddable
data class WorkingHours(
    val startTime: LocalTime,
    val endTime: LocalTime
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
