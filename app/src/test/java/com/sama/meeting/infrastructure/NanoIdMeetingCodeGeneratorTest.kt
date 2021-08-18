package com.sama.meeting.infrastructure

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class NanoIdMeetingCodeGeneratorTest {

    @Test
    fun `meeting code generated with at desired length`() {
        assertTrue(NanoIdMeetingCodeGenerator(10).generate().code.length == 10)
    }

    @Test
    fun `different meeting code generated each time`() {
        val generator1 = NanoIdMeetingCodeGenerator(10)
        val generator2 = NanoIdMeetingCodeGenerator(10)
        val code1 = generator1.generate()
        val code2 = generator1.generate()
        val code3 = generator2.generate()

        kotlin.test.assertNotEquals(code1, code2)
        kotlin.test.assertNotEquals(code2, code3)
    }
}