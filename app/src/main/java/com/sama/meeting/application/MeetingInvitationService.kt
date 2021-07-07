package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingInvitation
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.toUrl
import com.samskivert.mustache.Template
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofLocalizedDateTime
import java.time.format.DateTimeFormatter.ofLocalizedTime
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import java.time.format.TextStyle
import java.util.*

@Component
class MeetingInvitationService(
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    private val meetingProposalMessageTemplate: Template,
) {
    private val startDateTimeFormatter = ofLocalizedDateTime(MEDIUM, SHORT).withLocale(Locale.ENGLISH)
    private val endTimeFormatter = ofLocalizedTime(SHORT).withLocale(Locale.ENGLISH)

    fun findForProposedMeeting(proposedMeeting: ProposedMeeting, zoneId: ZoneId): MeetingInvitation {
        val meetingUrl = proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)
        val shareableMessage = meetingProposalMessageTemplate.execute(
            MeetingProposalMessageModel(
                proposedMeeting.proposedSlots.map {
                    val start = startDateTimeFormatter.format(it.startDateTime.withZoneSameInstant(zoneId)).removeYear()
                    val end = endTimeFormatter.format(it.endDateTime.withZoneSameInstant(zoneId))
                    val timeZone = zoneId.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH)
                    "$start - $end ($timeZone)"
                }, meetingUrl
            )
        )

        return MeetingInvitation(
            meetingUrl,
            shareableMessage
        )
    }

    private fun String.removeYear(): String {
        return this.replace(Regex(", \\d{4},"), "")
    }
}