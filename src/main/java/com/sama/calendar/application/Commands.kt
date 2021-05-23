package com.sama.calendar.application


data class InitiateMeetingCommand(val duration: Long, val recipientEmail: String?)
sealed class AddSuggestSlotCommand
data class ModifySuggestSlotCommand(val slot: ProposedSlotDTO)
data class RemoveSuggestSlotCommand(val slotId: Long)
data class ProposeMeetingCommand(val proposedSlots: List<ProposedSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: ProposedSlotDTO, val recipientEmail: String?)