package com.sama.calendar.application

import com.sama.calendar.domain.SlotId


data class InitiateMeetingCommand(val duration: Long, val recipientEmail: String?)
sealed class AddSuggestSlotCommand
data class ModifySuggestSlotCommand(val slot: ProposedSlotDTO)
data class RemoveSuggestSlotCommand(val slotId: Long)
data class ProposeMeetingCommand(val proposedSlots: Set<SlotId>)
data class ConfirmMeetingCommand(val meetingCode: String, val slotId: SlotId, val recipientEmail: String)