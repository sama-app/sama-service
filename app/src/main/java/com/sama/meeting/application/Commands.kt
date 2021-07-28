package com.sama.meeting.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sama.meeting.domain.MeetingIntentCode
import java.time.ZoneId
import javax.validation.constraints.Min

@JsonIgnoreProperties(ignoreUnknown = true)
data class InitiateMeetingCommand(
    @field:Min(15)
    val durationMinutes: Long,
    val timeZone: ZoneId,
    @field:Min(0)
    val suggestionSlotCount: Int,
)

data class ProposeMeetingCommand(val proposedSlots: List<MeetingSlotDTO>)
data class ProposeMeetingCommandV2(val meetingIntentCode: MeetingIntentCode, val proposedSlots: List<MeetingSlotDTO>)
data class ConfirmMeetingCommand(val slot: MeetingSlotDTO, val recipientEmail: String)