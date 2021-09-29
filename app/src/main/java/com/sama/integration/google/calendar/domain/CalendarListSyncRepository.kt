package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.users.domain.UserId
import java.time.Instant
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarListSyncRepository : Repository<CalendarListSync, Long> {
    fun find(googleAccountId: GoogleAccountId): CalendarListSync?
    fun findAndLock(googleAccountId: GoogleAccountId): CalendarListSync?

    fun findSyncable(from: Instant): Collection<GoogleAccountId>
    fun save(calendarListSync: CalendarListSync)
    fun deleteBy(accountId: GoogleAccountId)
}