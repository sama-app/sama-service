package com.sama.meeting.configuration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.CharStreams
import com.sama.meeting.domain.MeetingCode
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.InputStreamReader
import java.time.ZoneId
import java.util.Locale
import java.util.Locale.CANADA
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.Locale.ITALY
import java.util.Locale.JAPAN
import java.util.Locale.KOREA
import java.util.Locale.SIMPLIFIED_CHINESE
import java.util.Locale.TRADITIONAL_CHINESE
import java.util.Locale.UK
import java.util.Locale.US
import java.util.Locale.getAvailableLocales
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

    @Bean
    fun timeZonesToLocales(
        @Value("classpath:data/city-info.json") resource: Resource,
        objectMapper: ObjectMapper
    ): Map<ZoneId, Locale?> {
        val typeRef: TypeReference<List<CityInfo>> = object : TypeReference<List<CityInfo>>() {}
        val cityInfo = objectMapper.readValue(resource.inputStream, typeRef)

        val constantLocales =
            listOf(SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, FRANCE, GERMANY, ITALY, JAPAN, KOREA, UK, US, CANADA)
                .associateBy { it.country }
        val localesByCountry = getAvailableLocales().groupBy { it.country }

        return cityInfo
            .asSequence()
            .filter { it.timezone != null }
            .associateBy { it.timezone!! }
            .mapValues { (_, cityInfo) ->
                constantLocales[cityInfo.iso2] ?: localesByCountry[cityInfo.iso2]?.firstOrNull()
            }
    }
}

data class CityInfo(val city: String, val country: String, val timezone: ZoneId?, val iso2: String, val iso3: String)

data class MeetingProposalMessageModel(
    val showTimeZone: Boolean,
    val timeZone: String,
    val proposedSlots: Set<Map.Entry<String, List<String>>>,
    val meetingUrl: String
)