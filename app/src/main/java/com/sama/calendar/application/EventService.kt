package com.sama.calendar.application

import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class EventSearchCriteria(val createdFrom: ZonedDateTime?, val hasAttendees: Boolean?)

interface EventService {
    fun fetchEvents(
        userId: UserId,
        startDate: LocalDate,
        endDate: LocalDate,
        timezone: ZoneId,
    ) = fetchEvents(userId, startDate, endDate, timezone, EventSearchCriteria(null, null))

    fun fetchEvents(
        userId: UserId,
        startDate: LocalDate,
        endDate: LocalDate,
        timezone: ZoneId,
        searchCriteria: EventSearchCriteria
    ): FetchEventsDTO

    fun createEvent(userId: UserId, command: CreateEventCommand): EventDTO

    fun blockOutTimes(userId: UserId, command: BlockOutTimesCommand)
}