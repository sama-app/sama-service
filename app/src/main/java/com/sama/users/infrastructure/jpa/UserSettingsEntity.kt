package com.sama.users.infrastructure.jpa

import com.sama.common.Factory
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPermission
import com.sama.users.domain.UserSettings
import com.sama.users.domain.WorkingHours
import com.sama.users.infrastructure.toUserId
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.MapKey
import javax.persistence.OneToMany
import javax.persistence.Table
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters

@Entity
@Table(schema = "sama", name = "user_settings")
class UserSettingsEntity(
    @Id
    val userId: Long
) {

    @Factory
    companion object {
        fun new(userSettings: UserSettings): UserSettingsEntity {
            val entity = UserSettingsEntity(userId = userSettings.userId.id)
            entity.applyChanges(userSettings)
            return entity
        }
    }

    var locale: Locale? = null

    @Convert(converter = Jsr310JpaConverters.ZoneIdConverter::class)
    var timezone: ZoneId? = null

    @Column(name = "format_24_hour_time")
    var format24HourTime: Boolean? = null

    @Column(name = "past_event_contact_scan_enabled")
    var pastEventContactScanEnabled: Boolean? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, updatable = false, insertable = false)
    @MapKey(name = "dayOfWeek")
    var dayWorkingHours: MutableMap<DayOfWeek, DayWorkingHoursEntity> = mutableMapOf()

    var newsletterSubscriptionEnabledAt: Instant? = null

    var defaultMeetingTitle: String? = null

    var blockOutSuggestedSlots: Boolean? = null

    @LastModifiedDate
    var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "user_day_working_hours")
data class DayWorkingHoursEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    private val userId: Long,
    @Enumerated(EnumType.STRING)
    private val dayOfWeek: DayOfWeek,

    @Embedded
    var workingHours: WorkingHours
) {
    companion object {
        fun create(dayOfWeek: DayOfWeek, userId: UserId, workingHours: WorkingHours): DayWorkingHoursEntity {
            return DayWorkingHoursEntity(
                userId = userId.id,
                dayOfWeek = dayOfWeek,
                workingHours = workingHours
            )
        }
    }
}

fun UserSettingsEntity.applyChanges(userSettings: UserSettings): UserSettingsEntity {
    this.locale = userSettings.locale
    this.timezone = userSettings.timeZone
    this.format24HourTime = userSettings.format24HourTime
    userSettings.dayWorkingHours.forEach { (dayOfWeek, newWorkingHours) ->
        this.dayWorkingHours.compute(dayOfWeek)
        { _, value ->
            value
                ?.apply { this.workingHours = newWorkingHours }
                ?: DayWorkingHoursEntity.create(dayOfWeek, userId.toUserId(), newWorkingHours)
        }
    }
    this.dayWorkingHours.entries
        .removeIf { it.key !in userSettings.dayWorkingHours.keys }
    this.pastEventContactScanEnabled = userSettings.grantedPermissions.contains(UserPermission.PAST_EVENT_CONTACT_SCAN)

    if (newsletterSubscriptionEnabledAt != null && !userSettings.newsletterSubscriptionEnabled) {
        this.newsletterSubscriptionEnabledAt = null
    } else if (newsletterSubscriptionEnabledAt == null && userSettings.newsletterSubscriptionEnabled) {
        this.newsletterSubscriptionEnabledAt = Instant.now()
    }
    this.defaultMeetingTitle = userSettings.meetingPreferences.defaultTitle
    this.blockOutSuggestedSlots = userSettings.meetingPreferences.blockOutSlots
    this.updatedAt = Instant.now()
    return this
}