package com.sama.meeting.infrastructure.jpa

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentId
import com.sama.users.domain.UserId
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters

@AggregateRoot
@Entity
@Table(schema = "sama", name = "meeting_intent")
class MeetingIntentEntity {

    @Factory
    companion object {
        fun new(meetingIntent: MeetingIntent): MeetingIntentEntity {
            val meetingEntity = MeetingIntentEntity()
            meetingEntity.id = meetingIntent.meetingIntentId.id
            meetingEntity.code = UUID.randomUUID()
            meetingEntity.initiatorId = meetingIntent.initiatorId.id
            meetingEntity.durationMinutes = meetingIntent.duration.toMinutes()
            meetingEntity.timezone = meetingIntent.timezone
            val slots = meetingIntent.suggestedSlots.map {
                MeetingSuggestedSlotEntity(
                    null,
                    meetingIntent.meetingIntentId,
                    it.startDateTime,
                    it.endDateTime,
                    Instant.now()
                )
            }
            meetingEntity.suggestedSlots.addAll(slots)
            meetingEntity.createdAt = Instant.now()
            meetingEntity.updatedAt = Instant.now()
            return meetingEntity
        }
    }

    @Id
    var id: Long? = null

    @Column(nullable = false)
    var code: UUID? = null

    @Column(nullable = false)
    var initiatorId: Long? = null

    @Column(nullable = true)
    var recipientId: Long? = null

    @Column(nullable = false)
    var durationMinutes: Long? = null

    @Column(nullable = false)
    @Convert(converter = Jsr310JpaConverters.ZoneIdConverter::class)
    var timezone: ZoneId? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingIntentId", nullable = false, updatable = false, insertable = false)
    var suggestedSlots: MutableList<MeetingSuggestedSlotEntity> = mutableListOf()

    @CreatedDate
    var createdAt: Instant? = null

    @LastModifiedDate
    var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_suggested_slot")
data class MeetingSuggestedSlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    private val meetingIntentId: MeetingIntentId,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)
