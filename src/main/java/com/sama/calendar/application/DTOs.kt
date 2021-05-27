package com.sama.calendar.application

import com.sama.calendar.domain.*
import com.sama.users.domain.UserId
import liquibase.pro.packaged.it
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

fun MeetingEntity.toDTO(): MeetingDTO {
    return MeetingDTO(
        this.id!!,
        this.initiatorId!!,
        this.toRecipientDTO(),
        this.durationMinutes!!,
        this.slots.filter { it.status == MeetingSlotStatus.SUGGESTED }
            .map { it.toDTO() },
        this.slots.filter { it.status == MeetingSlotStatus.PROPOSED }
            .map { it.toDTO() },
        this.slots.filter { it.status == MeetingSlotStatus.CONFIRMED }
            .map { it.toDTO() }
            .firstOrNull()
    )
}

data class MeetingDTO(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val recipient: RecipientDTO,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>?,
    val proposedSlots: List<MeetingSlotDTO>?,
    val confirmedSlot: MeetingSlotDTO?
)

fun MeetingSlotEntity.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

data class MeetingSlotDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

fun MeetingEntity.toRecipientDTO(): RecipientDTO {
    return RecipientDTO(this.recipientId, this.recipientEmail)
}

fun MeetingSlotDTO.toValueObject(): MeetingSlot {
    return MeetingSlot(this.startDateTime, this.endDateTime)
}

data class RecipientDTO(
    val recipientId: UserId?,
    val recipientEmail: String?
)

data class ProposedMeetingDTO(
    val meetingId: MeetingId,
    val meetingCode: MeetingCode,
    val meetingUrl: String
)