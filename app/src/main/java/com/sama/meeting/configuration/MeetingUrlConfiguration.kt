package com.sama.meeting.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.meeting.url")
class MeetingUrlConfiguration(
    val codeLength: Int,
    val scheme: String,
    val host: String
)