package com.sama.users.domain

import com.sama.common.Factory
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.persistence.*

@Entity
@Table(schema = "sama", name = "user_settings")
class UserSettingsEntity(
    @Id
    val userId: Long
) {

    @Factory
    companion object {
        fun new(userSettings: UserSettings): UserSettingsEntity {
            val entity = UserSettingsEntity(userId = userSettings.userId)
            entity.applyChanges(userSettings)
            return entity
        }
    }

    var locale: Locale? = null

    @Convert(converter = Jsr310JpaConverters.ZoneIdConverter::class)
    var timezone: ZoneId? = null

    @Column(name = "format_24_hour_time")
    var format24HourTime: Boolean? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, updatable = false, insertable = false)
    @MapKey(name = "dayOfWeek")
    var dayWorkingHours: MutableMap<DayOfWeek, DayWorkingHoursEntity> = mutableMapOf()

    @LastModifiedDate
    var updatedAt: Instant? = null

    fun workingHours(): Map<DayOfWeek, WorkingHours> {
        return this.dayWorkingHours.mapValues { it.value.workingHours }
    }
}

@Entity
@Table(schema = "sama", name = "user_day_working_hours")
data class DayWorkingHoursEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    private val userId: Long,
    private val dayOfWeek: DayOfWeek,

    @Embedded
    var workingHours: WorkingHours
) {
    companion object {
        fun create(dayOfWeek: DayOfWeek, userId: Long, workingHours: WorkingHours): DayWorkingHoursEntity {
            return DayWorkingHoursEntity(
                userId = userId,
                dayOfWeek = dayOfWeek,
                workingHours = workingHours
            )
        }
    }
}

fun UserSettingsEntity.applyChanges(userSettings: UserSettings): UserSettingsEntity {
    this.locale = userSettings.locale
    this.timezone = userSettings.timezone
    this.format24HourTime = userSettings.format24HourTime
    userSettings.dayWorkingHours.forEach { (dayOfWeek, newWorkingHours) ->
        this.dayWorkingHours.compute(dayOfWeek)
        { _, value ->
            value
                ?.apply { this.workingHours = newWorkingHours }
                ?: DayWorkingHoursEntity.create(dayOfWeek, userId, newWorkingHours)
        }
    }
    this.dayWorkingHours.entries.removeIf { it.key !in dayWorkingHours.keys }
    this.updatedAt = Instant.now()
    return this
}