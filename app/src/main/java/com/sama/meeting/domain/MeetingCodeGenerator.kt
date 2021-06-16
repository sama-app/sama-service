package com.sama.meeting.domain

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.aventrix.jnanoid.jnanoid.NanoIdUtils.DEFAULT_ALPHABET
import com.aventrix.jnanoid.jnanoid.NanoIdUtils.DEFAULT_NUMBER_GENERATOR
import com.sama.common.DomainService
import com.sama.common.Factory
import com.sama.meeting.configuration.MeetingUrlConfiguration
import org.springframework.web.util.UriComponentsBuilder

@DomainService
data class MeetingCodeGenerator(val codeLength: Int) {

    @Factory
    fun generate(): MeetingCode {
        return NanoIdUtils.randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, codeLength)
    }
}

fun MeetingCode.toUrl(config: MeetingUrlConfiguration): String {
    return UriComponentsBuilder.newInstance()
        .scheme(config.scheme)
        .host(config.host)
        .path("/$this")
        .build().toUriString()
}