package com.sama.meeting.application

import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingSlot
import com.sama.users.application.UserPublicDTO
import com.sama.users.infrastructure.jpa.UserEntity
import java.time.ZonedDateTime

fun MeetingIntent.toDTO(): MeetingIntentDTO {
    return MeetingIntentDTO(
        this.code!!,
        this.duration.toMinutes(),
        this.suggestedSlots.map { it.toDTO() }
    )
}

data class MeetingIntentDTO(
    val meetingIntentCode: MeetingIntentCode,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>,
)

fun MeetingSlot.toDTO(): MeetingSlotDTO {
    return MeetingSlotDTO(this.startDateTime, this.endDateTime)
}

data class MeetingSlotDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

fun MeetingSlotDTO.toValueObject(): MeetingSlot {
    return MeetingSlot(this.startDateTime, this.endDateTime)
}

data class ProposedMeetingDTO(
    val proposedSlots: List<MeetingSlotDTO>,
    val initiator: UserPublicDTO,
    val isOwnMeeting: Boolean?,
    val title: String,
    val appLinks: MeetingAppLinksDTO?
)

data class MeetingAppLinksDTO(val iosAppDownloadLink: String)

data class MeetingDTO(
    val proposedSlots: List<MeetingSlotDTO>,
    val initiator: UserPublicDTO,
    val title: String
)

data class MeetingInvitationDTO(
    val meeting: MeetingDTO,
    val meetingCode: MeetingCode,
    val meetingUrl: String,
    val shareableMessage: String
)