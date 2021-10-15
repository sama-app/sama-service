package com.sama.meeting.domain

import com.sama.common.DomainEntity
import com.sama.users.domain.UserId

sealed interface Meeting {
    val meetingId: MeetingId
    val status: MeetingStatus
}

enum class MeetingStatus {
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

data class MeetingPreferences(
    val permanentLink: Boolean
) {
    companion object {
        fun default() = MeetingPreferences(false)
    }
}

@DomainEntity
data class ConfirmedMeeting(
    override val meetingId: MeetingId,
    val initiatorId: UserId,
    val recipient: MeetingRecipient,
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