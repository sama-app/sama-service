package com.sama.meeting.application

import java.time.ZoneId
import javax.validation.constraints.Min


data class InitiateMeetingCommand(
    @Min(0)
    val duration: Long,
    val timezone: ZoneId,
    @Min(0)
    val suggestionSlotCount: Int,
    @Min(0)
    val suggestionDayCount: Int,
)

data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val meetingCode: String, val slot: MeetingSlotDTO, val recipientEmail: String)