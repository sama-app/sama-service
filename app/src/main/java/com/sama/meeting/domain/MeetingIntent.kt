package com.sama.meeting.domain

import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.ZoneId

@DomainEntity
data class MeetingIntent(
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val recipientId: UserId?,
    val recipientTimeZone: ZoneId,
    val suggestedSlots: List<MeetingSlot>,
    val code: MeetingIntentCode? = null,
) {
    private val minimumDuration: Duration = Duration.ofMinutes(15)

    init {
        if (duration < minimumDuration) {
            throw InvalidDurationException(duration)
        }
        suggestedSlots.validate()
    }

    fun propose(
        meetingId: MeetingId,
        meetingCode: MeetingCode,
        proposedSlots: List<MeetingSlot>,
        meetingTitle: String,
    ): ProposedMeeting {
        if (!isSamaToSama() && proposedSlots.isEmpty()) {
            throw InvalidMeetingProposalException("No slots proposed")
        }
        proposedSlots.validate()

        return ProposedMeeting(
                meetingId,
                meetingIntentId,
                duration,
                initiatorId,
                recipientId,
                Actor.RECIPIENT,
                proposedSlots.combineContinuous(),
                emptyList(),
                meetingCode,
                meetingTitle
            )

    }

    fun isSamaToSama(): Boolean {
        return recipientId != null
    }

    fun isReadableBy(userId: UserId?): Boolean {
        return initiatorId == userId
    }

    private fun List<MeetingSlot>.combineContinuous(): List<MeetingSlot> {
        return fold(mutableListOf())
        { acc, slot ->
            val prevSlot = acc.lastOrNull()
            if (prevSlot != null && prevSlot.endDateTime.isEqual(slot.startDateTime)) {
                acc.removeLast()
                acc.add(MeetingSlot(prevSlot.startDateTime, slot.endDateTime))
            } else {
                acc.add(slot)
            }
            acc
        }
    }

    private fun List<MeetingSlot>.validate() {
        firstOrNull { it.duration() < duration }
            ?.run { throw InvalidMeetingSlotException(this) }
    }
}