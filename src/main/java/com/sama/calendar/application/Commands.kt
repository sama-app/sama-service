package com.sama.calendar.application

import java.time.ZoneId


data class InitiateMeetingCommand(
    val duration: Long,
    val timezone: ZoneId,
    val suggestionSlotCount: Int,
    val suggestionDayCount: Int,
)

data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: MeetingSlotDTO, val recipientEmail: String)