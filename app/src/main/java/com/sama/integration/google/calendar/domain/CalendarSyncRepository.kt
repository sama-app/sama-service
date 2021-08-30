package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.Instant
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarSyncRepository : Repository<CalendarSync, Long> {
    fun find(userId: UserId, calendarId: GoogleCalendarId): CalendarSync?
    fun findAndLock(userId: UserId, calendarId: GoogleCalendarId): CalendarSync?
    fun findSyncable(from: Instant): Collection<Pair<UserId, GoogleCalendarId>>
    fun save(calendarSync: CalendarSync)
}