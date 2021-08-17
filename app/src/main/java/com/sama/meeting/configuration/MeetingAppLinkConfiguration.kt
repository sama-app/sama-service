package com.sama.meeting.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.meeting.app-link")
class MeetingAppLinkConfiguration(
    val fqdn: String,
    val parameters: Map<String, String>
)