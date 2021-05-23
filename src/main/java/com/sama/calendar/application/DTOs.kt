package com.sama.calendar.application

import com.sama.calendar.domain.MeetingId
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

data class MeetingDTO(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val recipient: RecipientDTO,
    val duration: Int,
    val suggestedSlots: List<MeetingSlotDTO>?,
    val proposedSlots: List<MeetingSlotDTO>?,
    val confirmedSlot: MeetingSlotDTO?
)

data class RecipientDTO(val recipientId: UserId?, val recipientEmail: String?)

data class MeetingSlotDTO(
    val slot: Long,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val isRange: Boolean
)

data class ProposedSlotDTO(
    val slot: Long,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
)