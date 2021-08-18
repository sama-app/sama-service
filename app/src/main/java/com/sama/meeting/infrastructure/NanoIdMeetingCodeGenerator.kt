package com.sama.meeting.infrastructure

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingCodeGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class NanoIdMeetingCodeGenerator(
    @Value("\${sama.meeting.url.code-length}") val codeLength: Int
) : MeetingCodeGenerator {

    override fun generate(): MeetingCode {
        return NanoIdUtils.randomNanoId(
            NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
            NanoIdUtils.DEFAULT_ALPHABET,
            codeLength
        ).toMeetingCode()
    }
}