package com.sama.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.ZonedDateTime

@DomainRepository
interface EventRepository {
    fun findAll(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Event>

    fun save(userId: UserId, event: Event): Event
}