package com.sama.integration.google.calendar.domain

import com.google.common.math.IntMath.pow
import com.sama.common.to
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import org.threeten.extra.LocalDateRange

data class CalendarSync(
    val userId: UserId,
    val calendarId: GoogleCalendarId,
    val nextSyncAt: Instant,
    val failedSyncCount: Int,
    val syncToken: String? = null,
    val syncedRange: LocalDateRange? = null,
    val lastSynced: Instant? = null,
) {

    companion object {
        val syncInterval: Duration = Duration.ofSeconds(60)
        const val syncPastInMonths = 6L
        const val syncFutureInMonths = 3L
        const val fullSyncCutOffDays = 7L

        fun new(userId: UserId, calendarId: GoogleCalendarId, clock: Clock): CalendarSync {
            return CalendarSync(
                userId = userId,
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
        if (lastSynced!!.plus(syncInterval).isBefore(clock.instant())) {
            return false
        }

        if (needsFullSync(clock)) {
            return false
        }

        val requiredRange = startDateTime.withZoneSameInstant(UTC).toLocalDate() to
                endDateTime.withZoneSameInstant(UTC).toLocalDate()

        return syncedRange!!.encloses(requiredRange)
    }

    fun completeFull(syncToken: String, syncedRange: LocalDateRange, clock: Clock): CalendarSync {
        return copy(
            nextSyncAt = clock.instant().plus(syncInterval),
            failedSyncCount = 0,
            syncToken = syncToken,
            syncedRange = syncedRange,
            lastSynced = clock.instant()
        )
    }

    fun complete(syncToken: String, clock: Clock): CalendarSync {
        return copy(
            nextSyncAt = clock.instant().plus(syncInterval),
            failedSyncCount = 0,
            syncToken = syncToken,
            lastSynced = clock.instant()
        )
    }

    fun fail(clock: Clock): CalendarSync {
        val failCount = failedSyncCount + 1
        val nextSyncDelay = syncInterval.multipliedBy(pow(failCount, 2).toLong())
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
