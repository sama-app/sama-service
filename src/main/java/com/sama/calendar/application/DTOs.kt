package com.sama.calendar.application

import com.sama.calendar.domain.*
import com.sama.users.domain.UserId
import java.time.ZonedDateTime

data class BlockDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?
)

data class FetchBlocksDTO(
    val blocks: List<BlockDTO>
)

fun MeetingIntentEntity.toDTO(): MeetingIntentDTO {
    return MeetingIntentDTO(
        this.id!!,
        this.initiatorId!!,
        this.toRecipientDTO(),
        this.durationMinutes!!,
        this.suggestedSlots.map { it.toDTO() }
    )
}

data class MeetingIntentDTO(
    val meetingIntentId: MeetingProposalId,
    val initiatorId: UserId,
    val recipient: RecipientDTO,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>,
)

fun MeetingSuggestedSlotEntity.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

data class MeetingSlotDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

fun MeetingIntentEntity.toRecipientDTO(): RecipientDTO {
    return RecipientDTO(this.recipientId, null)
}

fun MeetingSlotDTO.toValueObject(): MeetingSlot {
    return MeetingSlot(this.startDateTime, this.endDateTime)
}

data class RecipientDTO(
    val userId: UserId?,
    val email: String?
)

data class MeetingProposalDTO(
    val meetingIntentId: MeetingIntentId,
    val meetingProposalId: MeetingProposalId,
    val meetingCode: MeetingCode,
    val meetingUrl: String
)