package com.sama.common

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class JavaTimeExtensionsTest {
    private val fixedDate = LocalDate.of(2021, 6, 1)
    private val fixedClock = Clock.fixed(fixedDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)


    @TestFactory
    fun toGmtString() = listOf(
        ZoneId.of("Europe/Rome") to "GMT+2",
        ZoneId.of("Europe/Vilnius") to "GMT+3",
        ZoneId.of("Europe/London") to "GMT+1",
        ZoneId.of("America/Los_Angeles") to "GMT-7",
        ZoneId.of("Asia/Tokyo") to "GMT+9",
        ZoneId.of("UTC") to "GMT",
        ZoneId.of("UTC-10") to "GMT-10",
        ZoneId.of("GMT+0") to "GMT",
        ZoneId.of("GMT-10") to "GMT-10",

        ).map { (zoneId, gmtString) ->
        dynamicTest("$zoneId -> $gmtString") {
            assertThat(zoneId.toGmtString(fixedClock.instant())).isEqualTo(gmtString)
        }
    }

}