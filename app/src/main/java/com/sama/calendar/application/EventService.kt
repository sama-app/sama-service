package com.sama.calendar.application

import com.sama.users.domain.UserId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface EventService {
    fun fetchEvents(
        userId: UserId,
        startDate: LocalDate,
        endDate: LocalDate,
        timezone: ZoneId,
        createdFrom: ZonedDateTime?
    ): FetchEventsDTO

    fun createEvent(userId: UserId, command: CreateEventCommand): EventDTO
}