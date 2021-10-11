package com.sama.integration.google.calendar.domain

import com.google.common.math.IntMath.pow
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.users.domain.UserId
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
        val nextSyncInterval: Duration = Duration.ofHours(12)
        val syncRetryInterval: Duration = Duration.ofSeconds(60)


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

    fun complete(syncToken: String, clock: Clock): CalendarListSync {
        return copy(
            nextSyncAt = clock.instant().plus(nextSyncInterval),
            failedSyncCount = 0,
            syncToken = syncToken,
            lastSynced = clock.instant()
        )
    }

    fun fail(clock: Clock): CalendarListSync {
        val failCount = failedSyncCount + 1
        val nextSyncDelay = syncRetryInterval.multipliedBy(pow(failCount, 2).toLong())
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
