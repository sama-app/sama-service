package com.sama.meeting.application

import com.sama.common.findByIdOrThrow
import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.samskivert.mustache.Template
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofLocalizedDateTime
import java.time.format.DateTimeFormatter.ofLocalizedTime
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import java.util.*

@Component
class MeetingInvitationView(
    private val userRepository: UserJpaRepository,
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    private val meetingProposalMessageTemplate: Template,
) {
    private val startDateTimeFormatter = ofLocalizedDateTime(MEDIUM, SHORT).withLocale(Locale.ENGLISH)
    private val endTimeFormatter = ofLocalizedTime(SHORT).withLocale(Locale.ENGLISH)

    fun render(proposedMeeting: ProposedMeeting, zoneId: ZoneId): MeetingInvitationDTO {
        val meetingUrl = proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)
        val shareableMessage = meetingProposalMessageTemplate.execute(
            MeetingProposalMessageModel(
                proposedMeeting.proposedSlots.map {
                    val start = startDateTimeFormatter.format(it.startDateTime.withZoneSameInstant(zoneId)).removeYear()
                    val end = endTimeFormatter.format(it.endDateTime.withZoneSameInstant(zoneId))
                    val timeZone = zoneId.toGmtString(it.startDateTime.toInstant())
                    "$start - $end ($timeZone)"
                },
                meetingUrl
            )
        )

        val initiator = userRepository.findByIdOrThrow(proposedMeeting.initiatorId)

        return MeetingInvitationDTO(
            MeetingDTO(
                proposedMeeting.proposedSlots.map { it.toDTO() },
                initiator.toInitiatorDTO()
            ),
            proposedMeeting.meetingCode,
            meetingUrl,
            shareableMessage
        )
    }

    private fun String.removeYear(): String {
        return this.replace(Regex(", \\d{4},"), "")
    }

    private fun ZoneId.toGmtString(atDate: Instant): String {
        val offsetSeconds = this.rules.getOffset(atDate).totalSeconds
        return if (offsetSeconds.mod(3600) == 0) {
            "GMT+${offsetSeconds / 3600}"
        } else {
            "GMT+${offsetSeconds.toFloat() / 3600}"
        }
    }
}