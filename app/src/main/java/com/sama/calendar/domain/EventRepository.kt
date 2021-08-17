package com.sama.calendar.domain

import com.sama.common.DomainRepository
import java.time.ZonedDateTime

@DomainRepository
interface EventRepository {
    fun findAll(userId: Long, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Event>

    fun save(userId: Long, event: Event): Event
}