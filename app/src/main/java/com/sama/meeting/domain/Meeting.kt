package com.sama.meeting.domain

import com.sama.calendar.application.EventDTO
import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime

enum class MeetingStatus {
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

sealed interface Meeting {
    val meetingId: MeetingId
    val status: MeetingStatus
}

@DomainEntity
data class ProposedMeeting(
    override val meetingId: MeetingId,
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val proposedSlots: List<MeetingSlot>,
    val meetingCode: MeetingCode,
    val meetingTitle: String
) : Meeting {
    override val status = MeetingStatus.PROPOSED
    val slotInterval = Duration.ofMinutes(15)

    fun proposedSlotsRange(): Pair<ZonedDateTime, ZonedDateTime> {
        val start = proposedSlots.minOf { it.startDateTime }
        val end = proposedSlots.maxOf { it.endDateTime }
        return start to end
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

    fun availableSlots(exclusions: Collection<EventDTO>, clock: Clock): AvailableSlots {
        return AvailableSlots.of(this, exclusions, clock)
    }

    fun updateTitle(title: String): ProposedMeeting {
        return copy(meetingTitle = title)
    }

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): Result<ConfirmedMeeting> {
        return kotlin.runCatching {
            val confirmedSlot = expandedSlots().find { it == slot }
                ?: throw MeetingSlotUnavailableException(meetingCode, slot)

            ConfirmedMeeting(meetingId, initiatorId, duration, recipient, confirmedSlot, meetingTitle)
        }
    }
}

@DomainEntity
data class AvailableSlots(val proposedSlots: List<MeetingSlot>) {
    companion object {
        fun of(proposedMeeting: ProposedMeeting, exclusions: Collection<EventDTO>, clock: Clock): AvailableSlots {
            val now = ZonedDateTime.now(clock)
            return proposedMeeting.expandedSlots()
                .filter { slot ->
                    slot.startDateTime.isAfter(now) && !exclusions.any { slot.overlaps(it) }
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
    val meetingTitle: String
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