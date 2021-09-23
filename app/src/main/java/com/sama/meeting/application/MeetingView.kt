package com.sama.meeting.application

import com.sama.common.View
import com.sama.integration.firebase.DynamicLinkService
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.meeting.domain.SamaSamaProposedMeeting
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@View
@Component
class MeetingView(
    private val userService: UserService,
    private val dynamicLinkService: DynamicLinkService,
    private val urlConfiguration: MeetingUrlConfiguration,
) {
    fun render(currentUserId: UserId?, meeting: SamaNonSamaProposedMeeting): ProposedMeetingDTO {
        val initiator = userService.find(meeting.initiatorId)

        val meetingUrl = meeting.meetingCode.toUrl(urlConfiguration)
        val isOwnMeeting = currentUserId?.let { it == meeting.initiatorId }
        val dynamicAppLink = dynamicLinkService.generate(meeting.meetingCode.code, meetingUrl)

        return ProposedMeetingDTO(
            meeting.expandedSlots.map { it.toDTO() },
            initiator,
            null,
            isOwnMeeting,
            false,
            meeting.meetingTitle,
            MeetingAppLinksDTO(dynamicAppLink)
        )
    }

    fun render(currentUserId: UserId?, meeting: SamaSamaProposedMeeting): ProposedMeetingDTO {
        val users = userService.findAll(listOf(meeting.initiatorId, meeting.recipientId))

        val isOwnMeeting = currentUserId?.let { it == meeting.initiatorId || it == meeting.recipientId }
        val isReadOnly = !meeting.isModifiableBy(currentUserId)

        return ProposedMeetingDTO(
            meeting.expandedSlots.map { it.toDTO() },
            users[meeting.initiatorId]!!,
            users[meeting.recipientId],
            isOwnMeeting,
            isReadOnly,
            meeting.meetingTitle,
            null
        )
    }
}
