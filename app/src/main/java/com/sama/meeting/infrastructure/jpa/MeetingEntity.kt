package com.sama.meeting.infrastructure.jpa

import com.sama.common.Factory
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingRecipient
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.domain.UserId
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

@Entity
@Table(schema = "sama", name = "meeting")
class MeetingEntity {

    @Factory
    companion object {
        fun new(proposedMeeting: ProposedMeeting): MeetingEntity {
            val entity = MeetingEntity()
            entity.id = proposedMeeting.meetingId.id
            entity.code = proposedMeeting.meetingCode.code
            entity.meetingIntentId = proposedMeeting.meetingIntentId.id
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
        this.meetingRecipient = MeetingRecipientEntity(
            confirmedMeeting.meetingRecipient.recipientId?.id,
            confirmedMeeting.meetingRecipient.email
        )
        this.confirmedSlot = confirmedMeeting.slot
        val now = Instant.now()
        this.confirmedAt = now
        this.updatedAt = now
        return this
    }

    @Id
    var id: Long? = null

    @Column(nullable = false)
    var meetingIntentId: Long? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null

    @Column(name = "meeting_code")
    var code: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId", nullable = false, updatable = false, insertable = false)
    var proposedSlots: MutableList<MeetingProposedSlotEntity> = mutableListOf()

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "recipientId", column = Column(name = "recipient_id")),
        AttributeOverride(name = "email", column = Column(name = "recipient_email"))
    )
    var meetingRecipient: MeetingRecipientEntity? = null

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "startDateTime", column = Column(name = "confirmed_slot_start_date_time")),
        AttributeOverride(name = "endDateTime", column = Column(name = "confirmed_slot_end_date_time"))
    )
    var confirmedSlot: MeetingSlot? = null

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
    var id: Long? = null,
    private val meetingId: MeetingId,
    var startDateTime: ZonedDateTime,
    var endDateTime: ZonedDateTime,
    @CreatedDate
    var createdAt: Instant? = null
)

@Embeddable
data class MeetingRecipientEntity(val recipientId: Long?, val email: String?)