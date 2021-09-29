package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.Instant
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarSyncRepository : Repository<CalendarSync, Long> {
    fun find(accountId: GoogleAccountId, calendarId: GoogleCalendarId): CalendarSync?
    fun findAll(accountId: GoogleAccountId): Collection<CalendarSync>
    fun findAll(accountIds: Set<GoogleAccountId>): Collection<CalendarSync>
    fun findAllCalendarIds(accountId: GoogleAccountId): Collection<GoogleCalendarId>
    fun findAndLock(accountId: GoogleAccountId, calendarId: GoogleCalendarId): CalendarSync?
    fun findSyncable(from: Instant): Collection<Pair<GoogleAccountId, GoogleCalendarId>>
    fun save(calendarSync: CalendarSync)
    fun delete(accountId: GoogleAccountId, calendarId: GoogleCalendarId)
}