package com.sama.integration.google.calendar.domain

import com.google.common.math.IntMath.pow
import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import org.springframework.data.repository.Repository


@DomainRepository
interface CalendarSyncRepository : Repository<CalendarSync, Long> {
    fun find(userId: UserId, calendarId: GoogleCalendarId): CalendarSync?
    fun findAndLock(userId: UserId, calendarId: GoogleCalendarId): CalendarSync?
    fun findSyncable(from: Instant): Collection<Pair<UserId, GoogleCalendarId>>
    fun save(calendarSync: CalendarSync)
}

private val syncInterval: Duration = Duration.ofSeconds(60)

data class CalendarSync(
    val userId: UserId,
    val calendarId: GoogleCalendarId,
    val nextSyncAt: Instant,
    val failedSyncCount: Int,
    val syncToken: String? = null,
    val syncedFrom: LocalDate? = null,
    val syncedTo: LocalDate? = null,
    val lastSynced: Instant? = null,
) {

    companion object {
        fun new(userId: UserId, calendarId: GoogleCalendarId): CalendarSync {
            return CalendarSync(
                userId = userId,
                calendarId = calendarId,
                nextSyncAt = Instant.now(),
                failedSyncCount = 0
            )
        }
    }

    fun needsFullSync(clock: Clock): Boolean {
        val fullSyncCutOff = LocalDate.now(clock).plusMonths(3).minusWeeks(1)
        return syncToken == null || (syncedTo?.isBefore(fullSyncCutOff) ?: true)
    }

    fun syncRange(clock: Clock): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now(clock)
        return now.minusMonths(6) to now.plusMonths(3)
    }

    fun isSyncedFor(startDateTime: ZonedDateTime, endDateTime: ZonedDateTime, clock: Clock): Boolean {
        if (needsFullSync(clock)) {
            return false
        }

        if (lastSynced!!.plus(syncInterval).isBefore(clock.instant())) {
            return false
        }

        return !syncedFrom!!.atStartOfDay(UTC).isAfter(startDateTime)
                && syncedTo!!.atStartOfDay(UTC).isAfter(endDateTime)
    }

    fun completeFull(syncToken: String, syncedFrom: LocalDate, syncedTo: LocalDate): CalendarSync {
        return copy(
            nextSyncAt = Instant.now().plus(syncInterval),
            failedSyncCount = 0,
            syncToken = syncToken,
            syncedFrom = syncedFrom,
            syncedTo = syncedTo,
            lastSynced = Instant.now()
        )
    }

    fun complete(syncToken: String): CalendarSync {
        return copy(
            nextSyncAt = Instant.now().plus(syncInterval),
            failedSyncCount = 0,
            syncToken = syncToken,
            lastSynced = Instant.now()
        )
    }

    fun fail(): CalendarSync {
        val failCount = failedSyncCount + 1
        val nextSyncDelay = syncInterval.multipliedBy(pow(failCount, 2).toLong())
        return copy(
            failedSyncCount = failCount,
            nextSyncAt = Instant.now().plus(nextSyncDelay)
        )
    }

    fun forceSync(): CalendarSync {
        return copy(nextSyncAt = Instant.now())
    }

    fun reset(): CalendarSync {
        return copy(
            syncToken = null,
            syncedFrom = null,
            syncedTo = null,
            failedSyncCount = 0,
            nextSyncAt = Instant.now()
        )
    }
}
