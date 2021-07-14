package com.sama.meeting.application

import com.sama.meeting.domain.MeetingIntentCode
import java.time.ZoneId
import javax.validation.constraints.Min


data class InitiateMeetingCommand(
    @field:Min(15)
    val durationMinutes: Long,
    val timeZone: ZoneId,
    @field:Min(0)
    val suggestionSlotCount: Int,
    @field:Min(0)
    val suggestionDayCount: Int,
)

data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ProposeMeetingCommandV2(val meetingIntentCode: MeetingIntentCode, val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val slot: MeetingSlotDTO, val recipientEmail: String)