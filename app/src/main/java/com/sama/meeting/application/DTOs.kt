package com.sama.meeting.application

import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingSuggestedSlotEntity
import com.sama.users.domain.UserEntity
import java.time.ZonedDateTime

fun MeetingIntentEntity.toDTO(): MeetingIntentDTO {
    return MeetingIntentDTO(
        this.code!!,
        this.durationMinutes!!,
        this.suggestedSlots.map { it.toDTO() }
    )
}

data class MeetingIntentDTO(
    val meetingIntentCode: MeetingIntentCode,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>,
)

fun MeetingSuggestedSlotEntity.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

fun MeetingSlot.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

data class MeetingSlotDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

fun MeetingSlotDTO.toValueObject(): MeetingSlot {
    return MeetingSlot(this.startDateTime, this.endDateTime)
}

fun UserEntity.toInitiatorDTO(): InitiatorDTO {
    return InitiatorDTO(this.fullName, this.email)
}

data class InitiatorDTO(
    val fullName: String?,
    val email: String
)

data class ProposedMeetingDTO(
    val proposedSlots: List<MeetingSlotDTO>,
    val initiator: InitiatorDTO
)

data class MeetingInvitationDTO(
    val meeting: ProposedMeetingDTO,
    val meetingUrl: String,
    val shareableMessage: String
)