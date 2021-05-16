package com.sama.users.domain

import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters.ZoneIdConverter
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.persistence.*
import javax.persistence.GenerationType.IDENTITY


@Entity
@Table(schema = "sama", name = "user_settings")
data class UserSettings(
    @Id
    val userId: Long,

    val locale: Locale,

    @Convert(converter = ZoneIdConverter::class)
    val timezone: ZoneId,

    @Column(name = "format_24_hour_time")
    val format24HourTime: Boolean,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "userId", nullable = false, updatable = false, insertable = false)
    @MapKey(name = "dayOfWeek")
    val workingHours: Map<DayOfWeek, DayWorkingHours>,

    @LastModifiedDate
    val updatedAt: Instant
) {
    companion object {
        fun usingDefaults(userId: Long, defaults: UserSettingsDefaults): UserSettings {
            return UserSettings(
                userId,
                defaults.locale ?: Locale.ENGLISH,
                defaults.timezone ?: ZoneId.of("Etc/GMT"),
                defaults.format24HourTime ?: false,
                defaults.workingHours
                    ?.mapValues {
                        val (startTime, endTime) = it.value
                        DayWorkingHours(
                            userId = userId,
                            dayOfWeek = it.key,
                            startTime = startTime,
                            endTime = endTime
                        )
                    }
                    ?: listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                        .associateWith { DayWorkingHours.nineToFive(it, userId) },
                Instant.now()
            )
        }
    }
}

@Entity
@Table(schema = "sama", name = "user_day_working_hours")
data class DayWorkingHours(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Long? = null,
    val userId: Long,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    companion object {
        fun nineToFive(dayOfWeek: DayOfWeek, userId: Long): DayWorkingHours {
            return DayWorkingHours(
                userId = userId,
                dayOfWeek = dayOfWeek,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0)
            )
        }
    }
}


data class UserSettingsDefaults(
    val locale: Locale?,
    val timezone: ZoneId?,
    val format24HourTime: Boolean?,
    val workingHours: Map<DayOfWeek, Pair<LocalTime, LocalTime>>?
)