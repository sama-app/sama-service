package com.sama.meeting.domain

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.sama.common.DomainService
import com.sama.common.Factory

@DomainService
data class MeetingCodeGenerator(val codeLength: Int) {
    @Factory
    companion object {
        private const val defaultCodeLength = 10
        fun default(): MeetingCodeGenerator {
            return MeetingCodeGenerator(defaultCodeLength)
        }
    }

    fun generate(): String {
        return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, codeLength)
    }
}