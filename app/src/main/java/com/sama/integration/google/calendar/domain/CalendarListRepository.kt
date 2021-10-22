package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.integration.google.auth.domain.GoogleAccountId
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarListRepository : Repository<CalendarList, GoogleAccountId> {
    fun find(accountId: GoogleAccountId): CalendarList?
    fun findAll(accountIds: Collection<GoogleAccountId>): Collection<CalendarList>
    fun save(calendarList: CalendarList)
    fun delete(accountId: GoogleAccountId)
}