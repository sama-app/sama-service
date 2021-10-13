package com.sama.meeting.application

import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingSlot
import com.sama.users.application.UserPublicDTO
import java.time.ZonedDateTime

fun MeetingIntent.toDTO(meetingTitle: String): MeetingIntentDTO {
    return MeetingIntentDTO(
        code!!,
        duration.toMinutes(),
        suggestedSlots.toDTO(),
        meetingTitle
    )
}

fun MeetingSlot.toDTO() = MeetingSlotDTO(this.startDateTime, this.endDateTime)
fun Iterable<MeetingSlot>.toDTO() = map { it.toDTO() }

data class MeetingIntentDTO(
    val meetingIntentCode: MeetingIntentCode,
    val durationMinutes: Long,
    val suggestedSlots: List<MeetingSlotDTO>,
    val defaultMeetingTitle: String
)

data class MeetingSlotDTO(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
)

data class ProposedMeetingDTO(
    val proposedSlots: List<MeetingSlotDTO>,
    val initiator: UserPublicDTO,
    val recipient: UserPublicDTO?,
    val isOwnMeeting: Boolean?,
    val isReadOnly: Boolean,
    val title: String,
    val appLinks: MeetingAppLinksDTO?
)

data class MeetingSlotSuggestionDTO(
    val suggestedSlots: List<MeetingSlotDTO>,
    val rejectedSlots: List<MeetingSlotDTO>
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