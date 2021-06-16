package com.sama.meeting.domain.aggregates

import com.sama.calendar.domain.SlotId
import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentId
import com.sama.users.domain.UserId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@AggregateRoot
@Entity
@Table(schema = "sama", name = "meeting_intent")
class MeetingIntentEntity {

    @Factory
    companion object {
        fun new(meetingIntent: MeetingIntent): MeetingIntentEntity {
            val meetingEntity = MeetingIntentEntity()
            meetingEntity.id = meetingIntent.meetingIntentId
            meetingEntity.initiatorId = meetingIntent.initiatorId
            meetingEntity.durationMinutes = meetingIntent.duration.toMinutes()
            meetingEntity.timezone = meetingIntent.timezone
            val slots = meetingIntent.suggestedSlots.map {
                MeetingSuggestedSlotEntity(
                    null,
                    meetingIntent.meetingIntentId,
                    it.startTime,
                    it.endTime,
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
    var id: MeetingIntentId? = null

    @Column(nullable = false)
    var initiatorId: UserId? = null

    @Column(nullable = true)
    var recipientId: UserId? = null

    @Column(nullable = false)
    var durationMinutes: Long? = null

    @Column(nullable = false)
    @Convert(converter = Jsr310JpaConverters.ZoneIdConverter::class)
    var timezone: ZoneId? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingIntentId", nullable = false, updatable = false, insertable = false)
    var suggestedSlots: MutableList<MeetingSuggestedSlotEntity> = mutableListOf()

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_suggested_slot")
data class MeetingSuggestedSlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: SlotId? = null,
    private val meetingIntentId: MeetingIntentId,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)
