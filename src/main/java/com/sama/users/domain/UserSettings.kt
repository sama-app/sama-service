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
import javax.persistence.CascadeType.ALL
import javax.persistence.FetchType.EAGER
import javax.persistence.GenerationType.IDENTITY


@Entity
@Table(schema = "sama", name = "user_settings")
class UserSettings(
    @Id
    val userId: Long,

    var locale: Locale,

    @Convert(converter = ZoneIdConverter::class)
    var timezone: ZoneId,

    @Column(name = "format_24_hour_time")
    var format24HourTime: Boolean,

    @OneToMany(cascade = [ALL], orphanRemoval = true, fetch = EAGER)
    @JoinColumn(name = "userId", nullable = false, updatable = false, insertable = false)
    @MapKey(name = "dayOfWeek")
    private var dayWorkingHours: MutableMap<DayOfWeek, DayWorkingHours>,

    @LastModifiedDate
    private var updatedAt: Instant
) {
    companion object {
        fun createUsingDefaults(userId: Long, defaults: UserSettingsDefaults): UserSettings {
            return UserSettings(
                userId,
                defaults.locale ?: Locale.ENGLISH,
                defaults.timezone ?: ZoneId.of("Etc/GMT"),
                defaults.format24HourTime ?: false,
                defaults.workingHours
                    ?.mapValuesTo(mutableMapOf())
                    {
                        val (startTime, endTime) = it.value
                        DayWorkingHours(
                            userId = userId,
                            dayOfWeek = it.key,
                            workingHours = WorkingHours(startTime, endTime)
                        )
                    }

                    ?: listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                        .associateWithTo(mutableMapOf())
                        {
                            DayWorkingHours.nineToFive(it, userId)
                        },

                Instant.now()
            )
        }
    }

    fun workingHours(): Map<DayOfWeek, WorkingHours> {
        return this.dayWorkingHours.mapValues { it.value.workingHours }
    }

    fun updateWorkingHours(dayWorkingHours: Map<DayOfWeek, WorkingHours>): UserSettings {
        dayWorkingHours.forEach { (dayOfWeek, newWorkingHours) ->
            this.dayWorkingHours.compute(dayOfWeek)
            { _, value ->
                value
                    ?.apply { this.workingHours = newWorkingHours }
                    ?: DayWorkingHours.create(dayOfWeek, userId, newWorkingHours)
            }
        }

        this.dayWorkingHours.entries.removeIf { it.key !in dayWorkingHours.keys }
        this.updatedAt = Instant.now()
        return this
    }
}

@Entity
@Table(schema = "sama", name = "user_day_working_hours")
data class DayWorkingHours(
    @Id @GeneratedValue(strategy = IDENTITY)
    private val id: Long? = null,
    private val userId: Long,
    private val dayOfWeek: DayOfWeek,

    @Embedded
    var workingHours: WorkingHours
) {
    companion object {
        fun create(dayOfWeek: DayOfWeek, userId: Long, workingHours: WorkingHours): DayWorkingHours {
            return DayWorkingHours(
                userId = userId,
                dayOfWeek = dayOfWeek,
                workingHours = workingHours
            )
        }

        fun nineToFive(dayOfWeek: DayOfWeek, userId: Long): DayWorkingHours {
            return create(
                userId = userId,
                dayOfWeek = dayOfWeek,
                workingHours = WorkingHours(
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(17, 0)
                )
            )
        }

    }
}

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
}

data class UserSettingsDefaults(
    val locale: Locale?,
    val timezone: ZoneId?,
    val format24HourTime: Boolean?,
    val workingHours: Map<DayOfWeek, WorkingHours>?
)