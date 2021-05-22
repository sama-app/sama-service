package com.sama.calendar.application

import com.sama.calendar.domain.MeetingId
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class MeetingApplicationService {

    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingDTO {
        TODO("not implemented")
    }

    fun addSuggestedSlot(userId: UserId, meetingId: MeetingId, command: AddSuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun modifySuggestedSlot(userId: UserId, meetingId: MeetingId, command: ModifySuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun removeSuggestedSlot(userId: UserId, meetingId: MeetingId, command: RemoveSuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun proposeMeeting(userId: UserId, meetingId: MeetingId, command: ProposeMeetingCommand): Boolean {
        TODO("not implemented")
    }

    fun confirmMeeting(command: ConfirmMeetingCommand): Boolean {
        TODO("not implemented")
    }
}

data class InitiateMeetingCommand(val duration: Int, val recipientEmail: String?)
sealed class AddSuggestSlotCommand
data class ModifySuggestSlotCommand(val slot: ProposedSlotDTO)
data class RemoveSuggestSlotCommand(val slotId: Long)
data class ProposeMeetingCommand(val proposedSlots: List<ProposedSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: ProposedSlotDTO, val recipientEmail: String?)

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