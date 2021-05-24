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
    fun applyChanges(proposedMeeting: ProposedMeeting): MeetingEntity {
        this.status = proposedMeeting.status
        this.duration = proposedMeeting.duration
        this.initiatorId = proposedMeeting.initiatorId
        this.recipientId = proposedMeeting.meetingRecipient?.recipientId
        this.recipientEmail = proposedMeeting.meetingRecipient?.email

        val slotsMap = this.slots.associateBy { it.id!! }
        proposedMeeting.proposedSlots
            .forEach {
                val slot = slotsMap[it.meetingSlotId]!!
                slot.startDateTime = it.startTime
                slot.endDateTime = it.endTime
                slot.status = it.status
            }

        return this
    }

    fun applyChanges(confirmedMeeting: ConfirmedMeeting): MeetingEntity {
        this.status = confirmedMeeting.status
        this.duration = confirmedMeeting.duration
        this.initiatorId = confirmedMeeting.initiatorId
        this.recipientId = confirmedMeeting.meetingRecipient.recipientId
        this.recipientEmail = confirmedMeeting.meetingRecipient.email

        val slot = this.slots.find { it.id!! == confirmedMeeting.slot.meetingSlotId }!!
        slot.startDateTime = confirmedMeeting.slot.startTime
        slot.endDateTime = confirmedMeeting.slot.endTime
        slot.status = confirmedMeeting.slot.status
        this.updatedAt = Instant.now()
        return this
    }

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
    var id: MeetingId? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null

    @Column(nullable = false)
    var initiatorId: UserId? = null

    @Column(name = "duration_minutes", nullable = false)
    var duration: Duration? = null

    var recipientId: UserId? = null

    var recipientEmail: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId", nullable = false, updatable = false, insertable = false)
    var slots: MutableList<MeetingSlotEntity> = mutableListOf()

    @Column(name = "meeting_code")
    var code: MeetingCode? = null

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_slot")
data class MeetingSlotEntity(
    @Id
    val id: SlotId? = null,
    private val meetingId: MeetingId,
    @Enumerated(EnumType.STRING)
    var status: MeetingSlotStatus,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @LastModifiedDate
    var updatedAt: Instant? = null
)