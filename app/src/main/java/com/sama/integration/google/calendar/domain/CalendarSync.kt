package com.sama.integration.google.calendar.domain

import com.google.common.math.IntMath.pow
import com.sama.common.to
import com.sama.integration.google.SyncConfiguration
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import org.threeten.extra.LocalDateRange

data class CalendarSync(
    val accountId: GoogleAccountId,
    val calendarId: GoogleCalendarId,
    val nextSyncAt: Instant,
    val failedSyncCount: Int,
    val syncToken: String? = null,
    val syncedRange: LocalDateRange? = null,
    val lastSynced: Instant? = null,
) {
    companion object {
        const val syncPastInMonths = 6L
        const val syncFutureInMonths = 3L
        const val fullSyncCutOffDays = 7L

        fun new(accountId: GoogleAccountId, calendarId: GoogleCalendarId, clock: Clock): CalendarSync {
            return CalendarSync(
                accountId = accountId,
                calendarId = calendarId,
                nextSyncAt = clock.instant(),
                failedSyncCount = 0
            )
        }

        fun syncRange(clock: Clock): LocalDateRange {
            val now = LocalDate.now(clock)
            return now.minusMonths(syncPastInMonths) to now.plusMonths(syncFutureInMonths)
        }
    }

    fun needsSync(clock: Clock) = nextSyncAt.isBefore(clock.instant())

    fun needsFullSync(clock: Clock): Boolean {
        if (syncToken == null || syncedRange == null) {
            return true
        }

        val now = LocalDate.now(clock)
        val requiredRange = now.minusMonths(syncPastInMonths) to
                now.plusMonths(syncFutureInMonths).minusDays(fullSyncCutOffDays)

        return !syncedRange.encloses(requiredRange)
    }

    fun isSyncedFor(startDateTime: ZonedDateTime, endDateTime: ZonedDateTime, clock: Clock): Boolean {
        if (needsFullSync(clock)) {
            return false
        }

        val requiredRange = startDateTime.withZoneSameInstant(UTC).toLocalDate() to
                endDateTime.withZoneSameInstant(UTC).toLocalDate()

        return syncedRange!!.encloses(requiredRange)
    }

    fun complete(syncToken: String, syncedRange: LocalDateRange, config: SyncConfiguration, clock: Clock): CalendarSync {
        return copy(
            nextSyncAt = clock.instant().plusSeconds(config.pollingIntervalSeconds),
            failedSyncCount = 0,
            syncToken = syncToken,
            syncedRange = syncedRange,
            lastSynced = clock.instant()
        )
    }

    fun complete(syncToken: String, config: SyncConfiguration, clock: Clock): CalendarSync {
        return complete(syncToken, syncedRange!!, config, clock)
    }

    fun fail(config: SyncConfiguration, clock: Clock): CalendarSync {
        val failCount = failedSyncCount + 1
        val nextSyncDelay = Duration.ofSeconds(config.retryIntervalSeconds)
            .multipliedBy(pow(failCount, 2).toLong())
        return copy(
            failedSyncCount = failCount,
            nextSyncAt = clock.instant().plus(nextSyncDelay)
        )
    }

    fun forceSync(clock: Clock): CalendarSync {
        return copy(nextSyncAt = clock.instant())
    }

    fun reset(clock: Clock): CalendarSync {
        return copy(
            syncToken = null,
            syncedRange = null,
            failedSyncCount = 0,
            nextSyncAt = clock.instant()
        )
    }

}
