package com.sama.calendar.application

import java.time.ZoneId


data class InitiateMeetingCommand(val duration: Long, val suggestedSlotCount: Int, val timezone: ZoneId)
data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: MeetingSlotDTO, val recipientEmail: String)