package com.sama.meeting.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.ZoneId

@DomainEntity
data class MeetingIntent(
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val timezone: ZoneId,
    val suggestedSlots: List<MeetingSlot>,
    val code: MeetingIntentCode? = null
) {
    private val minimumDuration: Duration = Duration.ofMinutes(15)

    init {
        if (duration < minimumDuration) {
            throw InvalidDurationException(duration)
        }
        validateSlots(suggestedSlots)
    }

    fun propose(
        meetingId: MeetingId,
        meetingCode: MeetingCode,
        proposedSlots: List<MeetingSlot>
    ): Result<ProposedMeeting> {
        if (proposedSlots.isEmpty()) {
            return Result.failure(InvalidMeetingProposalException("No slots proposed"))
        }

        kotlin.runCatching { validateSlots(proposedSlots) }
            .onFailure { return Result.failure(it) }

        return Result.success(
            ProposedMeeting(
                meetingId,
                meetingIntentId,
                initiatorId,
                duration,
                proposedSlots,
                meetingCode
            )
        )
    }

    private fun validateSlots(slots: List<MeetingSlot>) {
        slots.firstOrNull { it.duration() < duration }
            ?.run { throw InvalidMeetingSlotException(this) }
    }
}