package com.sama.calendar.domain

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.users.domain.UserId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
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
            meetingEntity.durationMinutes = initiatedMeeting.duration.toMinutes()
            meetingEntity.createdAt = Instant.now()
            meetingEntity.updatedAt = Instant.now()
            val slots = initiatedMeeting.suggestedSlots.map {
                MeetingSlotEntity(
                    null,
                    initiatedMeeting.meetingId,
                    MeetingSlotStatus.SUGGESTED,
                    it.startTime,
                    it.endTime,
                    Instant.now()
                )
            }
            meetingEntity.slots.addAll(slots)
            return meetingEntity
        }
    }

    fun applyChanges(proposedMeeting: ProposedMeeting): MeetingEntity {
        this.status = proposedMeeting.status
        this.durationMinutes = proposedMeeting.duration.toMinutes()
        this.initiatorId = proposedMeeting.initiatorId
        this.recipientId = proposedMeeting.meetingRecipient?.recipientId
        this.recipientEmail = proposedMeeting.meetingRecipient?.email
        this.code = proposedMeeting.meetingCode

        val slots = proposedMeeting.proposedSlots.map {
            MeetingSlotEntity(
                null,
                proposedMeeting.meetingId,
                MeetingSlotStatus.PROPOSED,
                it.startTime,
                it.endTime,
                Instant.now()
            )
        }
        this.slots.addAll(slots)
        return this
    }

    fun applyChanges(confirmedMeeting: ConfirmedMeeting): MeetingEntity {
        this.status = confirmedMeeting.status
        this.durationMinutes = confirmedMeeting.duration.toMinutes()
        this.initiatorId = confirmedMeeting.initiatorId
        this.recipientId = confirmedMeeting.meetingRecipient.recipientId
        this.recipientEmail = confirmedMeeting.meetingRecipient.email

        val slot = MeetingSlotEntity(
            null,
            confirmedMeeting.meetingId,
            MeetingSlotStatus.CONFIRMED,
            confirmedMeeting.slot.startTime,
            confirmedMeeting.slot.endTime,
            Instant.now()
        )
        this.slots.add(slot)
        return this
    }


    @Id
    var id: MeetingId? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null

    @Column(nullable = false)
    var initiatorId: UserId? = null

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Long? = null

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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: SlotId? = null,
    private val meetingId: MeetingId,
    @Enumerated(EnumType.STRING)
    var status: MeetingSlotStatus,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)