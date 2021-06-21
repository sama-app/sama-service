package com.sama.meeting.domain.aggregates

import com.sama.calendar.domain.SlotId
import com.sama.common.Factory
import com.sama.meeting.domain.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(schema = "sama", name = "meeting")
class MeetingEntity {

    @Factory
    companion object {
        fun new(proposedMeeting: ProposedMeeting): MeetingEntity {
            val entity = MeetingEntity()
            entity.id = proposedMeeting.meetingId
            entity.code = proposedMeeting.meetingCode
            entity.meetingIntentId = proposedMeeting.meetingIntentId
            entity.createdAt = Instant.now()
            entity.updatedAt = Instant.now()
            entity.status = proposedMeeting.status
            val slots = proposedMeeting.proposedSlots.map {
                MeetingProposedSlotEntity(
                    null,
                    proposedMeeting.meetingId,
                    it.startDateTime,
                    it.endDateTime,
                    Instant.now()
                )
            }
            entity.proposedSlots.addAll(slots)
            return entity
        }
    }

    fun applyChanges(confirmedMeeting: ConfirmedMeeting): MeetingEntity {
        this.status = confirmedMeeting.status
        this.meetingRecipient = confirmedMeeting.meetingRecipient
        this.confirmedSlot = confirmedMeeting.slot
        val now = Instant.now()
        this.confirmedAt = now
        this.updatedAt = now
        return this
    }

    @Id
    var id: MeetingId? = null

    @Column(nullable = false)
    var meetingIntentId: MeetingIntentId? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null

    @Column(name = "meeting_code")
    var code: MeetingCode? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId", nullable = false, updatable = false, insertable = false)
    var proposedSlots: MutableList<MeetingProposedSlotEntity> = mutableListOf()


    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "recipientId", column = Column(name = "recipient_id")),
        AttributeOverride(name = "email", column = Column(name = "recipient_email"))
    )
    var meetingRecipient: MeetingRecipient? = null

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "startDateTime", column = Column(name = "confirmed_slot_start_date_time")),
        AttributeOverride(name = "endDateTime", column = Column(name = "confirmed_slot_end_date_time"))
    )
    var confirmedSlot: MeetingSlot? = null

    @CreatedDate
    var confirmedAt: Instant? = null

    @CreatedDate
    var createdAt: Instant? = null

    @LastModifiedDate
    var updatedAt: Instant? = null
}

@Entity
@Table(schema = "sama", name = "meeting_proposed_slot")
data class MeetingProposedSlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: SlotId? = null,
    private val meetingId: MeetingId,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)