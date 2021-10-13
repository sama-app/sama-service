package com.sama.meeting.domain

import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val isSamaSama = recipientId != null

    init {
        if (duration < MEETING_SLOT_INTERVAL) {
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
        return when (isSamaSama) {
            true -> SamaSamaProposedMeeting(
                meetingId,
                meetingIntentId,
                duration,
                initiatorId,
                recipientId!!,
                Actor.RECIPIENT,
                proposedSlots.combineContinuous(),
                emptyList(),
                meetingCode,
                meetingTitle
            )
            false -> SamaNonSamaProposedMeeting(
                meetingId,
                meetingIntentId,
                duration,
                initiatorId,
                proposedSlots.combineContinuous(),
                meetingCode,
                meetingTitle,
                ZonedDateTime.now()
            )
        }
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