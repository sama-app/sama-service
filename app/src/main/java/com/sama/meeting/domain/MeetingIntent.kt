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
    val recipientId: UserId?,
    val duration: Duration,
    val timezone: ZoneId,
    val suggestedSlots: List<MeetingSlot>,
) {
    val allowedDurations = setOf(Duration.ofMinutes(30), Duration.ofMinutes(45), Duration.ofMinutes(60))

    @Factory
    companion object {
        fun of(meetingIntentEntity: MeetingIntentEntity): Result<MeetingIntent> {
            return kotlin.runCatching {
                MeetingIntent(
                    meetingIntentEntity.id!!, meetingIntentEntity.initiatorId!!,
                    meetingIntentEntity.recipientId,
                    Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                    meetingIntentEntity.timezone!!,
                    meetingIntentEntity.suggestedSlots
                        .map { MeetingSlot(it.startDateTime, it.endDateTime) }
                )
            }
        }
    }

    init {
        if (duration !in allowedDurations) {
            throw InvalidDurationException(meetingIntentId, duration)
        }
        validateSlots(suggestedSlots)
    }

    fun propose(
        meetingProposalId: MeetingProposalId,
        meetingCode: MeetingCode,
        proposedSlots: List<MeetingSlot>
    ): Result<ProposedMeeting> {
        if (proposedSlots.isEmpty()) {
            return Result.failure(InvalidMeetingProposalException(meetingIntentId, "No slots proposed"))
        }

        kotlin.runCatching { validateSlots(proposedSlots) }
            .onFailure { return Result.failure(it) }

        return Result.success(
            ProposedMeeting(
                meetingProposalId,
                meetingIntentId,
                initiatorId,
                duration,
                proposedSlots,
                meetingCode
            )
        )
    }

    private fun validateSlots(slots: List<MeetingSlot>) {
        // TODO: validate duplicates

        slots.firstOrNull { it.duration() < duration }
            ?.run {
                throw InvalidMeetingSlotException(meetingIntentId, this)
            }
    }
}