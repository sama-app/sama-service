package com.sama.integration.google.calendar.domain

import com.sama.common.to
import com.sama.integration.google.SyncConfiguration
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class CalendarSyncTest {
    private val fixedDate = LocalDate.of(2021, 6, 1)
    private val fixedClock = Clock.fixed(fixedDate.atStartOfDay().toInstant(UTC), UTC)
    private val syncedRange = CalendarSync.syncRange(fixedClock)
    private val syncConfiguration = SyncConfiguration(60, 60)

    private val nonSynced = CalendarSync.new(GoogleAccountId(1L), "primary", fixedClock)
    private val fullySynced = nonSynced
        .complete("token", syncedRange, syncConfiguration, fixedClock)

    private val partiallySyncedStart = nonSynced
        .complete("token", fixedDate to fixedDate.plusYears(6), syncConfiguration, fixedClock)

    private val partiallySyncedEnd = nonSynced
        .complete("token", fixedDate.minusYears(6) to fixedDate, syncConfiguration, fixedClock)


    @TestFactory
    fun `needs full sync`() = listOf(
        nonSynced to true,
        fullySynced to false,
        partiallySyncedStart to true,
        partiallySyncedEnd to true
    ).map { (calendarSync, expected) ->
        dynamicTest("${calendarSync.syncedRange} needs full sync: $expected") {
            assertThat(calendarSync.needsFullSync(fixedClock)).isEqualTo(expected)
        }
    }


    @TestFactory
    fun `is synced for date range`() = listOf(
        Pair(fixedDate.atStartOfDay(UTC), fixedDate.plusDays(1).atStartOfDay(UTC)) to true,
        Pair(syncedRange.start.atStartOfDay(UTC), syncedRange.end.atStartOfDay(UTC)) to true,
        Pair(fixedDate.atStartOfDay(UTC), fixedDate.plusYears(1).atStartOfDay(UTC)) to false,
        Pair(fixedDate.minusYears(1).atStartOfDay(UTC), fixedDate.atStartOfDay(UTC)) to false,
    ).map { (requiredRange, expected) ->
        dynamicTest("$requiredRange is synced: $expected") {
            assertThat(fullySynced.isSyncedFor(requiredRange.first, requiredRange.second, fixedClock))
                .isEqualTo(expected)
        }
    }

    @TestFactory
    fun `is synced for current time`() = listOf(
        fixedClock to true,
        Clock.offset(fixedClock, Duration.ofSeconds(59)) to true,
        Clock.offset(fixedClock, Duration.ofHours(24)) to true,
    ).map { (clock, expected) ->
        dynamicTest("${clock.instant()} is synced: $expected") {
            assertThat(fullySynced.isSyncedFor(ZonedDateTime.now(clock), ZonedDateTime.now(clock), clock))
                .isEqualTo(expected)
        }
    }
}