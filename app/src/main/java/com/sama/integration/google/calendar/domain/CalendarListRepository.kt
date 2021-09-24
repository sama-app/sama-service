package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarListRepository : Repository<CalendarList, UserId> {
    fun find(userId: UserId): CalendarList?
    fun save(calendarList: CalendarList)
}