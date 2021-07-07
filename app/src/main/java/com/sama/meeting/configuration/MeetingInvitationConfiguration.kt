package com.sama.meeting.configuration

import com.google.common.io.CharStreams
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingSlot
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.util.UriComponentsBuilder
import java.io.InputStreamReader

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.meeting.url")
class MeetingUrlConfiguration(
    val codeLength: Int,
    val scheme: String,
    val host: String
)

@Configuration
class MeetingProposalMessageConfiguration {

    @Bean
    fun meetingProposalMessageTemplate(@Value("classpath:templates/meeting-proposal.mustache") resource: Resource): Template {
        val template = CharStreams.toString(InputStreamReader(resource.inputStream, Charsets.UTF_8))
        return Mustache.compiler().compile(template)
    }
}

data class MeetingProposalMessageModel(val proposedSlots: List<String>, val meetingUrl: String)