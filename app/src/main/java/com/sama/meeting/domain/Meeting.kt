package com.sama.meeting.domain

import com.sama.calendar.application.EventDTO
import com.sama.calendar.domain.Event
import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.Result.Companion.success

sealed interface Meeting {
    val meetingId: MeetingId
    val status: MeetingStatus
}

@Factory
fun meetingFrom(
    meetingIntentEntity: MeetingIntentEntity,
    meetingEntity: MeetingEntity
): Result<Meeting> {
    return when (meetingEntity.status!!) {
        MeetingStatus.PROPOSED -> {
            val proposedSlots = meetingEntity.proposedSlots
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            success(
                ProposedMeeting(
                    meetingEntity.id!!,
                    meetingIntentEntity.id!!,
                    meetingIntentEntity.initiatorId!!,
                    Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                    proposedSlots,
                    meetingEntity.code!!,
                )
            )
        }
        MeetingStatus.CONFIRMED -> success(
            ConfirmedMeeting(
                meetingEntity.id!!,
                meetingIntentEntity.initiatorId!!,
                Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                meetingEntity.meetingRecipient!!,
                meetingEntity.confirmedSlot!!
            )
        )
        MeetingStatus.REJECTED -> success(RejectedMeeting(meetingEntity.id!!))
        MeetingStatus.EXPIRED -> success(ExpiredMeeting(meetingEntity.id!!))
    }
}


enum class MeetingStatus {
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

@DomainEntity
data class ProposedMeeting(
    override val meetingId: MeetingId,
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val proposedSlots: List<MeetingSlot>,
    val meetingCode: MeetingCode,
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

    fun availableProposedSlots(exclusions: Collection<EventDTO>, clock: Clock): List<MeetingSlot> {
        val now = ZonedDateTime.now(clock)
        return expandedSlots()
            .filter { slot ->
                slot.startDateTime.isAfter(now) && !exclusions.any { slot.overlaps(it) }
            }
    }

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): Result<ConfirmedMeeting> {
        return kotlin.runCatching {
            val confirmedSlot = expandedSlots().find { it == slot }
                ?: throw MeetingSlotUnavailableException(meetingCode, slot)

            ConfirmedMeeting(meetingId, initiatorId, duration, recipient, confirmedSlot)
        }
    }
}

@DomainEntity
data class ConfirmedMeeting(
    override val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val meetingRecipient: MeetingRecipient,
    val slot: MeetingSlot
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