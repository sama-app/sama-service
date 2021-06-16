package com.sama.meeting.domain

import com.sama.calendar.domain.Block
import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.NotFoundException
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingProposalEntity
import com.sama.meeting.domain.aggregates.MeetingSuggestedSlotEntity
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.Result.Companion.success

sealed interface Meeting {
    val meetingProposalId: MeetingProposalId
    val status: MeetingStatus
}

@Factory
fun meetingFrom(
    meetingIntentEntity: MeetingIntentEntity,
    meetingProposalEntity: MeetingProposalEntity
): Result<Meeting> {
    return when (meetingProposalEntity.status!!) {
        MeetingStatus.PROPOSED -> {
            val proposedSlots = meetingProposalEntity.proposedSlots
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            success(
                ProposedMeeting(
                    meetingProposalEntity.id!!,
                    meetingIntentEntity.id!!,
                    meetingIntentEntity.initiatorId!!,
                    Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                    proposedSlots,
                    meetingProposalEntity.code!!,
                )
            )
        }
        MeetingStatus.CONFIRMED -> success(
            ConfirmedMeeting(
                meetingProposalEntity.id!!,
                meetingIntentEntity.initiatorId!!,
                Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                meetingProposalEntity.meetingRecipient!!,
                meetingProposalEntity.confirmedSlot!!
            )
        )
        MeetingStatus.REJECTED -> success(RejectedMeeting(meetingProposalEntity.id!!))
        MeetingStatus.EXPIRED -> success(ExpiredMeeting(meetingProposalEntity.id!!))
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
    override val meetingProposalId: MeetingProposalId,
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val proposedSlots: List<MeetingSlot>,
    val meetingCode: MeetingCode,
) : Meeting {
    override val status = MeetingStatus.PROPOSED
    val slotInterval = Duration.ofMinutes(15)

    @Factory
    companion object {
        fun of(
            meetingIntentEntity: MeetingIntentEntity,
            meetingProposalEntity: MeetingProposalEntity
        ): Result<ProposedMeeting> {
            val proposedSlots = meetingProposalEntity.proposedSlots
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            return success(
                ProposedMeeting(
                    meetingProposalEntity.id!!,
                    meetingIntentEntity.id!!,
                    meetingIntentEntity.initiatorId!!,
                    Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                    proposedSlots,
                    meetingProposalEntity.code!!,
                )
            )
        }
    }

    fun proposedSlotsRange(): Pair<ZonedDateTime, ZonedDateTime> {
        val start = proposedSlots.minOf { it.startTime }
        val end = proposedSlots.maxOf { it.endTime }
        return start to end
    }

    fun expandedSlots(): List<MeetingSlot> {
        return proposedSlots.flatMap { slot ->
            if (!slot.isRange(duration)) {
                slot.expandBy(duration, slotInterval)
                    .map { MeetingSlot(it, it.plus(duration)) }
            } else {
                listOf(slot)
            }
        }
    }

    fun availableProposedSlots(exclusions: Collection<Block>, clock: Clock): List<MeetingSlot> {
        val now = ZonedDateTime.now(clock)
        return expandedSlots()
            .filter { slot ->
                slot.startTime.isAfter(now) && !exclusions.any { slot.overlaps(it) }
            }
    }

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): Result<ConfirmedMeeting> {
        return kotlin.runCatching {
            val confirmedSlot = expandedSlots().find { it == slot }
                ?: throw MeetingSlotUnavailableException(meetingProposalId, slot)

            ConfirmedMeeting(meetingProposalId, initiatorId, duration, recipient, confirmedSlot)
        }
    }
}

@DomainEntity
data class ConfirmedMeeting(
    override val meetingProposalId: MeetingProposalId,
    val initiatorId: UserId,
    val duration: Duration,
    val meetingRecipient: MeetingRecipient,
    val slot: MeetingSlot
) : Meeting {
    override val status = MeetingStatus.CONFIRMED
}

@DomainEntity
data class RejectedMeeting(override val meetingProposalId: MeetingProposalId) : Meeting {
    override val status = MeetingStatus.CONFIRMED
}

@DomainEntity
data class ExpiredMeeting(override val meetingProposalId: MeetingProposalId) : Meeting {
    override val status = MeetingStatus.CONFIRMED
}