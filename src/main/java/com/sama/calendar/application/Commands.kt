package com.sama.calendar.application


data class InitiateMeetingCommand(val duration: Long, val suggestedSlotCount: Int)
data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: MeetingSlotDTO, val recipientEmail: String)