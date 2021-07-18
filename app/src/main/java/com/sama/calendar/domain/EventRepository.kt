package com.sama.calendar.domain

import java.time.ZonedDateTime

interface EventRepository {
    fun findAll(userId: Long, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Event>

    fun save(userId: Long, event: Event): Event
}