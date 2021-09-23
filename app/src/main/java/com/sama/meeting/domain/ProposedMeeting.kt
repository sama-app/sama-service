package com.sama.meeting.domain

import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.time.Duration

sealed interface ProposedMeeting : Meeting {
    override val status: MeetingStatus
        get() = MeetingStatus.PROPOSED

    val meetingIntentId: MeetingIntentId
    val meetingCode: MeetingCode
    val initiatorId: UserId
    val duration: Duration
    val meetingTitle: String
    val proposedSlots: List<MeetingSlot>
    val expandedSlots: List<MeetingSlot>
        get() = proposedSlots.flatMap { slot ->
            if (slot.isRange(duration)) {
                slot.expandBy(duration, MEETING_SLOT_INTERVAL)
                    .map { MeetingSlot(it, it.plus(duration)) }
            } else {
                listOf(slot)
            }
        }

    fun isReadableBy(userId: UserId?): Boolean
    fun isModifiableBy(userId: UserId?): Boolean

    fun updateTitle(title: String): ProposedMeeting

    fun List<MeetingSlot>.validate() {
        firstOrNull { it.duration() < duration }
            ?.run { throw InvalidMeetingSlotException(this) }
    }
}

@DomainEntity
data class SamaNonSamaProposedMeeting(
    override val meetingId: MeetingId,
    override val meetingIntentId: MeetingIntentId,
    override val duration: Duration,
    override val initiatorId: UserId,
    override val proposedSlots: List<MeetingSlot>,
    override val meetingCode: MeetingCode,
    override val meetingTitle: String,
) : ProposedMeeting {

    init {
        if (proposedSlots.isEmpty()) {
            throw InvalidMeetingProposalException("No slots proposed")
        }
        proposedSlots.validate()
    }

    override fun isReadableBy(userId: UserId?) = true
    override fun isModifiableBy(userId: UserId?) = true
    override fun updateTitle(title: String) = copy(meetingTitle = title)

    fun claimAsRecipient(recipientId: UserId): SamaSamaProposedMeeting {
        check(recipientId != initiatorId) { "Cannot claim one's own meeting" }
        return SamaSamaProposedMeeting(
            meetingId, meetingIntentId, duration, initiatorId, recipientId, Actor.RECIPIENT,
            proposedSlots, emptyList(), meetingCode, meetingTitle
        )
    }

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): ConfirmedMeeting {
        val confirmedSlot = expandedSlots.find { it == slot }
            ?: throw MeetingSlotUnavailableException(meetingCode, slot)
        return ConfirmedMeeting(meetingId, initiatorId, recipient, confirmedSlot, meetingTitle)
    }

}

enum class Actor {
    INITIATOR,
    RECIPIENT
}

@DomainEntity
data class SamaSamaProposedMeeting(
    override val meetingId: MeetingId,
    override val meetingIntentId: MeetingIntentId,
    override val duration: Duration,
    override val initiatorId: UserId,
    val recipientId: UserId,
    val currentActor: Actor,
    override val proposedSlots: List<MeetingSlot>,
    val rejectedSlots: List<MeetingSlot>,
    override val meetingCode: MeetingCode,
    override val meetingTitle: String,
) : ProposedMeeting {
    val isInvitation = proposedSlots.isEmpty()

    init {
        proposedSlots.validate()
        rejectedSlots.validate()
    }

    override fun isReadableBy(userId: UserId?) = initiatorId == userId || recipientId == userId
    override fun isModifiableBy(userId: UserId?) = currentActorId() == userId
    override fun updateTitle(title: String): SamaSamaProposedMeeting = copy(meetingTitle = title)

    fun proposeNewSlots(newSlots: List<MeetingSlot>): SamaSamaProposedMeeting {
        val previouslyRejectedSlots = rejectedSlots.intersect(newSlots)
        val newlyRejectedSlots = proposedSlots.subtract(newSlots)

        return copy(
            proposedSlots = newSlots,
            rejectedSlots = rejectedSlots.minus(previouslyRejectedSlots).plus(newlyRejectedSlots),
            currentActor = nextActor()
        )
    }

    fun confirm(slot: MeetingSlot): ConfirmedMeeting {
        val confirmedSlot = expandedSlots.find { it == slot }
            ?: throw MeetingSlotUnavailableException(meetingCode, slot)
        return ConfirmedMeeting(
            meetingId, initiatorId, UserRecipient.of(recipientId), confirmedSlot, meetingTitle
        )
    }

    private fun currentActorId() = when (currentActor) {
        Actor.INITIATOR -> initiatorId
        Actor.RECIPIENT -> recipientId
    }

    private fun nextActor() = when (currentActor) {
        Actor.INITIATOR -> Actor.RECIPIENT
        Actor.RECIPIENT -> Actor.INITIATOR
    }
}