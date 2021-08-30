package com.sama.meeting.application

import com.sama.integration.firebase.DynamicLinkService
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class MeetingView(
    private val userService: UserService,
    private val dynamicLinkService: DynamicLinkService,
    private val urlConfiguration: MeetingUrlConfiguration,
) {
    fun render(
        currentUserId: UserId?,
        proposedMeeting: ProposedMeeting,
        slots: List<MeetingSlot>,
    ): ProposedMeetingDTO {
        val initiator = userService.find(proposedMeeting.initiatorId)

        val meetingUrl = proposedMeeting.meetingCode.toUrl(urlConfiguration)
        val isOwnMeeting = currentUserId?.let { it == proposedMeeting.initiatorId }
        val dynamicAppLink = dynamicLinkService.generate(meetingUrl)

        return ProposedMeetingDTO(
            slots.map { it.toDTO() },
            initiator,
            isOwnMeeting,
            proposedMeeting.meetingTitle,
            MeetingAppLinksDTO(dynamicAppLink)
        )
    }
}