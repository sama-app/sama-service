package com.sama.calendar.domain

import com.sama.calendar.domain.MeetingStatus.INITIATED
import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.time.Duration

enum class MeetingStatus {
    INITIATED,
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED
}

enum class MeetingSlotStatus {
    SUGGESTED,
    PROPOSED,
    REJECTED
}

@DomainEntity
data class InitiatedMeeting(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val suggestedSlots: List<Any>
) {
    val status = INITIATED

    fun suggestSlot(slot: Any): InitiatedMeeting {
        return copy(suggestedSlots = suggestedSlots.plus(slot))
    }
}