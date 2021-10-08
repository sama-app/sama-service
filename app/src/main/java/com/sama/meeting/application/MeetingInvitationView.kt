package com.sama.meeting.application

import com.sama.common.View
import com.sama.common.toGmtString
import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.application.UserService
import com.samskivert.mustache.Template
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofLocalizedDateTime
import java.time.format.DateTimeFormatter.ofLocalizedTime
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import java.util.Locale
import org.springframework.stereotype.Component

@View
@Component
class MeetingInvitationView(
    private val userService: UserService,
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    private val meetingProposalMessageTemplate: Template,
) {
    private val startDateTimeFormatter = ofLocalizedDateTime(MEDIUM, SHORT).withLocale(Locale.ENGLISH)
    private val endTimeFormatter = ofLocalizedTime(SHORT).withLocale(Locale.ENGLISH)

    fun render(proposedMeeting: ProposedMeeting, zoneId: ZoneId): MeetingInvitationDTO {
        val meetingUrl = proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)
        val sortedProposedSots = proposedMeeting.proposedSlots.sortedBy { it.startDateTime }

        val shareableMessage = meetingProposalMessageTemplate.execute(
            MeetingProposalMessageModel(
                sortedProposedSots.map {
                    val start = startDateTimeFormatter.format(it.startDateTime.withZoneSameInstant(zoneId)).removeYear()
                    val end = endTimeFormatter.format(it.endDateTime.withZoneSameInstant(zoneId))
                    val timeZone = zoneId.toGmtString(it.startDateTime.toInstant())
                    "$start - $end ($timeZone)"
                },
                meetingUrl
            )
        )

        val initiator = userService.find(proposedMeeting.initiatorId)

        return MeetingInvitationDTO(
            MeetingDTO(
                sortedProposedSots.map { it.toDTO() },
                initiator,
                proposedMeeting.meetingTitle
            ),
            proposedMeeting.meetingCode,
            meetingUrl,
            shareableMessage
        )
    }

    private fun String.removeYear(): String {
        return this.replace(Regex(", \\d{4},"), "")
    }
}