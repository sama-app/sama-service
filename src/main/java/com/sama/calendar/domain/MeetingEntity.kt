package com.sama.calendar.domain

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.users.domain.UserId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.*

@AggregateRoot
@Entity
@Table(schema = "sama", name = "meeting")
class MeetingEntity {

    @Factory
    companion object {
        fun new(initiatedMeeting: InitiatedMeeting): MeetingEntity {
            val meetingEntity = MeetingEntity()
            meetingEntity.id = initiatedMeeting.meetingId
            meetingEntity.status = initiatedMeeting.status
            meetingEntity.initiatorId = initiatedMeeting.initiatorId
            meetingEntity.duration = initiatedMeeting.duration
            meetingEntity.createdAt = Instant.now()
            meetingEntity.updatedAt = Instant.now()

            return meetingEntity
        }
    }


    @Id
    private var id: MeetingId? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private var status: MeetingStatus? = null

    @Column(nullable = false)
    private var initiatorId: UserId? = null

    @Column(name = "duration_minutes", nullable = false)
    private var duration: Duration? = null

    private var recipientEmail: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId", nullable = false, updatable = false, insertable = false)
    private var slots: MutableList<MeetingSlotEntity> = mutableListOf()

    @Column(name = "meeting_code")
    private var code: MeetingCode? = null

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_slot")
data class MeetingSlotEntity(
    @Id
    private val id: SlotId? = null,
    private val meetingId: MeetingId,
    @Enumerated(EnumType.STRING)
    val status: MeetingSlotStatus,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    @LastModifiedDate
    var updatedAt: Instant? = null
)