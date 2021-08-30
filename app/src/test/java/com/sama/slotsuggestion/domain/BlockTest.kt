package com.sama.slotsuggestion.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime


internal class BlockTest {

    @Test
    fun testZeroDuration() {
        val now = ZonedDateTime.now()
        assertThat(Block(now, now, false, false, 0).zeroDuration())
            .isTrue()

        assertThat(Block(now, now.plusMinutes(1), false, false, 0).zeroDuration())
            .isFalse()
    }
}