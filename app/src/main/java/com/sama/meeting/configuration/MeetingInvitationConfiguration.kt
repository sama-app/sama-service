package com.sama.meeting.configuration

import com.google.common.io.CharStreams
import com.sama.meeting.domain.MeetingCode
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.InputStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.util.UriComponentsBuilder

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.meeting.url")
class MeetingUrlConfiguration(
    val codeLength: Int,
    val scheme: String,
    val host: String
)

fun MeetingCode.toUrl(config: MeetingUrlConfiguration): String {
    return UriComponentsBuilder.newInstance()
        .scheme(config.scheme)
        .host(config.host)
        .path("/${this.code}")
        .build().toUriString()
}

@Configuration
class MeetingProposalMessageConfiguration {

    @Bean
    fun meetingProposalMessageTemplate(@Value("classpath:templates/meeting-proposal.mustache") resource: Resource): Template {
        val template = CharStreams.toString(InputStreamReader(resource.inputStream, Charsets.UTF_8))
        return Mustache.compiler().compile(template)
    }
}

data class MeetingProposalMessageModel(
    val showTimeZone: Boolean,
    val timeZone: String,
    val proposedSlots: Set<Map.Entry<String, List<String>>>,
    val meetingUrl: String
)