package com.sama.meeting.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sama.meeting.domain.MeetingIntentCode
import java.time.ZoneId
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class InitiateMeetingCommand(
    @field:Min(15)
    val durationMinutes: Long,
    val timeZone: ZoneId,
    @field:Min(0)
    val suggestionSlotCount: Int,
)

data class ProposeMeetingCommand(
    val meetingIntentCode: MeetingIntentCode,
    val proposedSlots: List<MeetingSlotDTO>,
)

data class UpdateMeetingTitleCommand(
    @field:NotBlank
    val title: String
)

data class ConfirmMeetingCommand(
    val slot: MeetingSlotDTO,
    val recipientEmail: String?,
)