package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingInvitation
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.toUrl
import com.samskivert.mustache.Template
import org.springframework.stereotype.Component

@Component
class MeetingInvitationService(
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    private val meetingProposalMessageTemplate: Template,
) {

    fun findForProposedMeeting(proposedMeeting: ProposedMeeting): MeetingInvitation {
        val meetingUrl = proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)
        val shareableMessage = meetingProposalMessageTemplate.execute(
            MeetingProposalMessageModel(proposedMeeting.proposedSlots, meetingUrl)
        )

        return MeetingInvitation(
            meetingUrl,
            shareableMessage
        )
    }
}