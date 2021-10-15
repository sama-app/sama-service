package com.sama.meeting.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingSlot
import com.sama.users.domain.UserPublicId
import java.time.ZoneId
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class InitiateMeetingCommand(
    @field:Min(15)
    val durationMinutes: Long,
    val timeZone: ZoneId? = null,
    val recipientId: UserPublicId? = null,
) {
    fun asSamaNonSama(): InitiateSamaNonSamaMeetingCommand {
        check(timeZone != null)
        return InitiateSamaNonSamaMeetingCommand(durationMinutes, timeZone)
    }

    fun asSamaSama(): InitiateSamaSamaMeetingCommand {
        check(recipientId != null)
        return InitiateSamaSamaMeetingCommand(durationMinutes, recipientId)
    }
}

data class InitiateSamaNonSamaMeetingCommand(
    val durationMinutes: Long,
    val timeZone: ZoneId,
)

data class InitiateSamaSamaMeetingCommand(
    val durationMinutes: Long,
    val recipientId: UserPublicId,
)

data class ProposeMeetingCommand(
    val meetingIntentCode: MeetingIntentCode,
    val proposedSlots: List<MeetingSlotDTO>,
    val title: String? = null
)

data class CreateFullAvailabilityLinkCommand(
    val durationMinutes: Long,
    val meetingCode: String
)


data class ProposeNewMeetingSlotsCommand(
    val proposedSlots: List<MeetingSlotDTO>
)

data class UpdateMeetingTitleCommand(
    @field:NotBlank
    val title: String,
)

object ConnectWithMeetingInitiatorCommand

data class ConfirmMeetingCommand(
    val slot: MeetingSlotDTO,
    val recipientEmail: String?,
)

fun MeetingSlotDTO.toValueObject(): MeetingSlot {
    return MeetingSlot(this.startDateTime, this.endDateTime)
}