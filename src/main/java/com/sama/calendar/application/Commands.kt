package com.sama.calendar.application

import com.sama.calendar.domain.SlotId


data class InitiateMeetingCommand(val duration: Long, val recipientEmail: String?, val suggestedSlotCount: Int)
data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: MeetingSlotDTO, val recipientEmail: String)