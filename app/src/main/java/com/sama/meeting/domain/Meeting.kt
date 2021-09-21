package com.sama.meeting.domain

import com.sama.calendar.application.EventDTO
import com.sama.common.DomainEntity
import com.sama.meeting.domain.Actor.INITIATOR
import com.sama.meeting.domain.Actor.RECIPIENT
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import liquibase.pro.packaged.it

enum class MeetingStatus {
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

enum class Actor {
    INITIATOR,
    RECIPIENT
}

sealed interface Meeting {
    val meetingId: MeetingId
    val status: MeetingStatus
}

@DomainEntity
data class ProposedMeeting(
    override val meetingId: MeetingId,
    val meetingIntentId: MeetingIntentId,
    val duration: Duration,
    val initiatorId: UserId,
    val recipientId: UserId?,
    val currentActor: Actor,
    val proposedSlots: List<MeetingSlot>,
    val rejectedSlots: List<MeetingSlot>,
    val meetingCode: MeetingCode,
    val meetingTitle: String,
) : Meeting {
    override val status = MeetingStatus.PROPOSED
    private val slotInterval = Duration.ofMinutes(15)

    fun isReadableBy(userId: UserId?): Boolean {
        return currentActorId() == null || initiatorId == userId || recipientId == userId
    }

    fun isModifiableBy(userId: UserId?): Boolean {
        return currentActorId() == null || currentActorId() == userId
    }

    fun expandedSlots(): List<MeetingSlot> {
        return proposedSlots.flatMap { slot ->
            if (slot.isRange(duration)) {
                slot.expandBy(duration, slotInterval)
                    .map { MeetingSlot(it, it.plus(duration)) }
            } else {
                listOf(slot)
            }
        }
    }

    fun isSamaToSama(): Boolean {
        return recipientId != null
    }

    fun updateTitle(title: String): ProposedMeeting {
        return copy(meetingTitle = title)
    }

    fun claimAsRecipient(recipientId: UserId): ProposedMeeting {
        check(!isSamaToSama()) { "Meeting is already claimed" }
        check(recipientId != initiatorId) { "Cannot claim one's own meeting" }
        return copy(recipientId = recipientId)
    }

    fun proposeNewSlots(newSlots: List<MeetingSlot>): ProposedMeeting {
        newSlots.validate()

        val previouslyRejectedSlots = rejectedSlots.intersect(newSlots)
        val newlyRejectedSlots = proposedSlots.subtract(newSlots)

        return copy(
            proposedSlots = newSlots,
            rejectedSlots = rejectedSlots.minus(previouslyRejectedSlots).plus(newlyRejectedSlots),
            currentActor = nextActor()
        )
    }

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): ConfirmedMeeting {
        val confirmedSlot = expandedSlots().find { it == slot }
            ?: throw MeetingSlotUnavailableException(meetingCode, slot)
        return ConfirmedMeeting(meetingId, initiatorId, duration, recipient, confirmedSlot, meetingTitle)
    }

    private fun currentActorId(): UserId? {
        return when (currentActor) {
            INITIATOR -> initiatorId
            RECIPIENT -> recipientId
        }
    }

    private fun nextActor(): Actor {
        return if (currentActor == INITIATOR) {
            RECIPIENT
        } else {
            INITIATOR
        }
    }

    private fun List<MeetingSlot>.validate() {
        firstOrNull { it.duration() < duration }
            ?.run { throw InvalidMeetingSlotException(this) }
    }

    fun proposedSlotsRange(): Pair<ZonedDateTime, ZonedDateTime> {
        val start = proposedSlots.minOf { it.startDateTime }
        val end = proposedSlots.maxOf { it.endDateTime }
        return start to end
    }

    fun availableSlots(exclusions: Collection<EventDTO>, clock: Clock): AvailableSlots {
        return AvailableSlots.of(this, exclusions, clock)
    }
}

data class AvailableSlots(val slots: List<MeetingSlot>) {
    companion object {
        fun of(proposedMeeting: ProposedMeeting, exclusions: Collection<EventDTO>, clock: Clock): AvailableSlots {
            val now = ZonedDateTime.now(clock)
            return proposedMeeting.expandedSlots()
                .filter { slot ->
                    slot.endDateTime.isAfter(now) && !exclusions.any { slot.overlaps(it) }
                }
                .let { AvailableSlots(it) }
        }
    }
}

@DomainEntity
data class ConfirmedMeeting(
    override val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val meetingRecipient: MeetingRecipient,
    val slot: MeetingSlot,
    val meetingTitle: String,
) : Meeting {
    override val status = MeetingStatus.CONFIRMED
}

@DomainEntity
data class RejectedMeeting(override val meetingId: MeetingId) : Meeting {
    override val status = MeetingStatus.REJECTED
}

@DomainEntity
data class ExpiredMeeting(override val meetingId: MeetingId) : Meeting {
    override val status = MeetingStatus.EXPIRED
}