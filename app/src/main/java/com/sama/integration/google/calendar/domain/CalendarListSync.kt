package com.sama.integration.google.calendar.domain

import com.google.common.math.IntMath.pow
import com.sama.integration.google.SyncConfiguration
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class CalendarListSync(
    val accountId: GoogleAccountId,
    val nextSyncAt: Instant,
    val failedSyncCount: Int,
    val syncToken: String? = null,
    val lastSynced: Instant? = null,
) {
    companion object {
        fun new(accountId: GoogleAccountId, clock: Clock): CalendarListSync {
            return CalendarListSync(
                accountId = accountId,
                nextSyncAt = clock.instant(),
                failedSyncCount = 0
            )
        }
    }

    fun needsSync(clock: Clock) = nextSyncAt.isBefore(clock.instant())

    fun needsFullSync() = syncToken == null

    fun complete(syncToken: String, config: SyncConfiguration, clock: Clock): CalendarListSync {
        return copy(
            nextSyncAt = clock.instant().plusSeconds(config.pollingIntervalSeconds),
            failedSyncCount = 0,
            syncToken = syncToken,
            lastSynced = clock.instant()
        )
    }

    fun fail(config: SyncConfiguration, clock: Clock): CalendarListSync {
        val failCount = failedSyncCount + 1
        val nextSyncDelay = Duration.ofSeconds(config.retryIntervalSeconds)
            .multipliedBy(pow(failCount, 2).toLong())
        return copy(
            failedSyncCount = failCount,
            nextSyncAt = clock.instant().plus(nextSyncDelay)
        )
    }

    fun forceSync(clock: Clock): CalendarListSync {
        return copy(nextSyncAt = clock.instant())
    }

    fun reset(clock: Clock): CalendarListSync {
        return copy(
            syncToken = null,
            failedSyncCount = 0,
            nextSyncAt = clock.instant()
        )
    }
}
