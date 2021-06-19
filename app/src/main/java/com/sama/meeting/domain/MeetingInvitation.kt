package com.sama.meeting.domain

import com.sama.meeting.configuration.MeetingUrlConfiguration
import org.springframework.web.util.UriComponentsBuilder

data class MeetingInvitation(
    val url: String,
    val message: String
)

fun MeetingCode.toUrl(config: MeetingUrlConfiguration): String {
    return UriComponentsBuilder.newInstance()
        .scheme(config.scheme)
        .host(config.host)
        .path("/$this")
        .build().toUriString()
}

