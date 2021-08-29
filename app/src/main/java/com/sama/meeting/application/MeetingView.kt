package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingAppLinkConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class MeetingView(
    private val userService: UserService,
    private val urlConfiguration: MeetingUrlConfiguration,
    private val appLinkConfiguration: MeetingAppLinkConfiguration,
) {

    fun render(
        currentUserId: UserId?,
        proposedMeeting: ProposedMeeting,
        slots: List<MeetingSlot>,
    ): ProposedMeetingDTO {
        val initiator = userService.find(proposedMeeting.initiatorId)

        val meetingUrl = proposedMeeting.meetingCode.toUrl(urlConfiguration)
        val isOwnMeeting = currentUserId?.let { it == proposedMeeting.initiatorId }
        val appLinks = createAppLinks(meetingUrl)

        return ProposedMeetingDTO(
            slots.map { it.toDTO() },
            initiator,
            isOwnMeeting,
            proposedMeeting.meetingTitle,
            appLinks
        )
    }

    // Manually constructed Firebase Dynamic Link
    // https://firebase.google.com/docs/dynamic-links/create-manually#parameters
    private fun createAppLinks(meetingUrl: String): MeetingAppLinksDTO {
        val urlBuilder = UriComponentsBuilder.newInstance()
            .scheme("https")
            .host(appLinkConfiguration.fqdn)
            .path("/")
            .queryParam("link", meetingUrl)
        for ((key, value) in appLinkConfiguration.parameters.entries) {
            if (value.isEmpty()) continue
            urlBuilder.queryParam(key, value)
        }

        return MeetingAppLinksDTO(
            iosAppDownloadLink = urlBuilder.build().toUriString()
        )
    }
}