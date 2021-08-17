package com.sama.meeting.application

import com.sama.common.findByIdOrThrow
import com.sama.meeting.configuration.MeetingAppLinkConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.AvailableSlots
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.toUrl
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class MeetingView(
    private val userRepository: UserRepository,
    private val urlConfiguration: MeetingUrlConfiguration,
    private val appLinkConfiguration: MeetingAppLinkConfiguration
) {

    fun render(proposedMeeting: ProposedMeeting, availableSlots: AvailableSlots): ProposedMeetingDTO {
        val initiatorEntity = userRepository.findByIdOrThrow(proposedMeeting.initiatorId)

        val meetingUrl = proposedMeeting.meetingCode.toUrl(urlConfiguration)
        val appLinks = createAppLinks(meetingUrl)

        val slots = availableSlots.proposedSlots.map { it.toDTO() }
        return ProposedMeetingDTO(
            slots,
            initiatorEntity.toInitiatorDTO(),
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
            urlBuilder.queryParam(key, value)
        }

        return MeetingAppLinksDTO(
            iosAppDownloadLink = urlBuilder.build().toUriString()
        )
    }
}