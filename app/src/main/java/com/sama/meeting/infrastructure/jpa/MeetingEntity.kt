package com.sama.meeting.infrastructure.jpa

import com.sama.common.Factory
import com.sama.meeting.domain.Actor
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.meeting.domain.SamaSamaProposedMeeting
import com.sama.meeting.domain.UserRecipient
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
        fun new(proposedMeeting: SamaNonSamaProposedMeeting): MeetingEntity {
            val entity = MeetingEntity()
            entity.id = proposedMeeting.meetingId.id
            entity.code = proposedMeeting.meetingCode.code
            entity.meetingIntentId = proposedMeeting.meetingIntentId.id
            entity.meetingRecipient = null
            entity.currentActor = Actor.RECIPIENT
            entity.createdAt = Instant.now()
            entity.updatedAt = Instant.now()
            entity.status = proposedMeeting.status
            entity.title = proposedMeeting.meetingTitle
            entity.permanentLink = proposedMeeting.meetingPreferences.permanentLink
            val proposedSlots = proposedMeeting.proposedSlots.map {
                it.toEntity(proposedMeeting.meetingId, MeetingSlotStatus.PROPOSED)
            }
            entity.proposedSlots.addAll(proposedSlots)
            return entity
        }

        fun new(proposedMeeting: SamaSamaProposedMeeting): MeetingEntity {
            val entity = MeetingEntity()
            entity.id = proposedMeeting.meetingId.id
            entity.code = proposedMeeting.meetingCode.code
            entity.meetingIntentId = proposedMeeting.meetingIntentId.id
            entity.meetingRecipient = MeetingRecipientEntity(proposedMeeting.recipientId.id, null)
            entity.currentActor = proposedMeeting.currentActor
            entity.status = proposedMeeting.status
            entity.title = proposedMeeting.meetingTitle
            entity.permanentLink = proposedMeeting.meetingPreferences.permanentLink
            val proposedSlots = proposedMeeting.proposedSlots.map {
                it.toEntity(proposedMeeting.meetingId, MeetingSlotStatus.PROPOSED)
            }
            val rejectedSlots = proposedMeeting.rejectedSlots.map {
                it.toEntity(proposedMeeting.meetingId, MeetingSlotStatus.REJECTED)
            }
            entity.proposedSlots.addAll(proposedSlots)
            entity.proposedSlots.addAll(rejectedSlots)
            val now = Instant.now()
            entity.createdAt = now
            entity.updatedAt = now
            return entity
        }
    }

    fun applyChanges(proposedMeeting: SamaNonSamaProposedMeeting): MeetingEntity {
        title = proposedMeeting.meetingTitle
        permanentLink = proposedMeeting.meetingPreferences.permanentLink
        val now = Instant.now()
        this.confirmedAt = now
        this.updatedAt = now
        return this
    }

    fun applyChanges(proposedMeeting: SamaSamaProposedMeeting): MeetingEntity {
        title = proposedMeeting.meetingTitle
        permanentLink = proposedMeeting.meetingPreferences.permanentLink
        meetingRecipient = MeetingRecipientEntity(proposedMeeting.recipientId.id, null)
        currentActor = proposedMeeting.currentActor
        val proposedSlots = proposedMeeting.proposedSlots.map {
            it.toEntity(proposedMeeting.meetingId, MeetingSlotStatus.PROPOSED)
        }
        val rejectedSlots = proposedMeeting.rejectedSlots.map {
            it.toEntity(proposedMeeting.meetingId, MeetingSlotStatus.REJECTED)
        }
        this.proposedSlots.clear()
        this.proposedSlots.addAll(proposedSlots)
        this.proposedSlots.addAll(rejectedSlots)
        val now = Instant.now()
        this.confirmedAt = now
        this.updatedAt = now
        return this
    }

    fun applyChanges(confirmedMeeting: ConfirmedMeeting): MeetingEntity {
        this.status = confirmedMeeting.status
        this.meetingRecipient = when (val recipient = confirmedMeeting.recipient) {
            is EmailRecipient -> MeetingRecipientEntity(null, recipient.email)
            is UserRecipient -> MeetingRecipientEntity(recipient.recipientId.id, null)
        }
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

    @Column(nullable = false)
    var title: String? = null

    @Column
    var permanentLink: Boolean? = null

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var currentActor: Actor? = null

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
class MeetingProposedSlotEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    private val meetingId: MeetingId,

    @Column(nullable = false)
    var startDateTime: ZonedDateTime,

    @Column(nullable = false)
    var endDateTime: ZonedDateTime,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MeetingSlotStatus,

    @CreatedDate
    @Column(nullable = false)
    var createdAt: Instant? = null,
)

enum class MeetingSlotStatus {
    PROPOSED, REJECTED
}

@Embeddable
data class MeetingRecipientEntity(val recipientId: Long?, val email: String?)

private fun MeetingSlot.toEntity(meetingId: MeetingId, status: MeetingSlotStatus): MeetingProposedSlotEntity {
    return MeetingProposedSlotEntity(
        null,
        meetingId,
        startDateTime,
        endDateTime,
        status,
        Instant.now()
    )
}