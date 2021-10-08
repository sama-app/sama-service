package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.Instant
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarListSyncRepository : Repository<CalendarListSync, Long> {
    fun find(userId: UserId): CalendarListSync?
    fun findAndLock(userId: UserId): CalendarListSync?
    fun findSyncable(from: Instant): Collection<UserId>
    fun save(calendarListSync: CalendarListSync)
}