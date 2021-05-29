package com.sama.calendar.domain

import com.sama.common.Factory
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.*


@Entity
@Table(schema = "sama", name = "meeting_proposal")
class MeetingProposalEntity {

    @Factory
    companion object {
        fun new(meetingProposal: MeetingProposal): MeetingProposalEntity {
            val entity = MeetingProposalEntity()
            entity.id = meetingProposal.meetingProposalId
            entity.code = meetingProposal.meetingCode
            entity.meetingIntentId = meetingProposal.meetingIntentId
            entity.createdAt = Instant.now()
            entity.updatedAt = Instant.now()
            entity.status = meetingProposal.status
            val slots = meetingProposal.proposedSlots.map {
                MeetingProposedSlotEntity(
                    null,
                    meetingProposal.meetingProposalId,
                    it.startTime,
                    it.endTime,
                    Instant.now()
                )
            }
            entity.proposedSlots.addAll(slots)
            return entity
        }
    }

    fun applyChanges(confirmedMeeting: ConfirmedMeeting): MeetingProposalEntity {
        this.status = confirmedMeeting.status
        this.meetingRecipient = confirmedMeeting.meetingRecipient
        this.confirmedSlot = confirmedMeeting.slot
        val now = Instant.now()
        this.confirmedAt = now
        this.updatedAt = now
        return this
    }

    @Id
    var id: MeetingProposalId? = null

    @Column(nullable = false)
    var meetingIntentId: MeetingIntentId? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingProposalStatus? = null

    @Column(name = "meeting_code")
    var code: MeetingCode? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingProposalId", nullable = false, updatable = false, insertable = false)
    var proposedSlots: MutableList<MeetingProposedSlotEntity> = mutableListOf()


    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "recipientId", column = Column(name = "recipient_id")),
        AttributeOverride(name = "email", column = Column(name = "recipient_email"))
    )
    var meetingRecipient: MeetingRecipient? = null

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "startTime", column = Column(name = "confirmed_slot_start_date_time")),
        AttributeOverride(name = "endTime", column = Column(name = "confirmed_slot_end_date_time"))
    )
    var confirmedSlot: MeetingSlot? = null

    @CreatedDate
    private var confirmedAt: Instant? = null

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_proposed_slot")
data class MeetingProposedSlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: SlotId? = null,
    private val meetingProposalId: MeetingProposalId,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)