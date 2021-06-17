package com.sama.meeting.application

import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingSuggestedSlotEntity
import com.sama.users.domain.UserId
import java.time.ZonedDateTime

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
    val meetingIntentId: MeetingId,
    val initiatorId: UserId,
    val recipient: RecipientDTO,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>,
)

fun MeetingSuggestedSlotEntity.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

fun MeetingSlot.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startTime, this.endTime)
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

data class MeetingDTO(
    val meetingIntentId: MeetingIntentId,
    val meetingId: MeetingId,
    val proposedSlots: List<MeetingSlotDTO>,
    val meetingCode: MeetingCode,
    val meetingUrl: String,
)