package com.sama.calendar.application

import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class EventSearchCriteria(val createdFrom: ZonedDateTime?, val hasAttendees: Boolean?)

interface EventService {
    fun fetchEvents(startDate: LocalDate, endDate: LocalDate, timezone: ZoneId) =
        fetchEvents(startDate, endDate, timezone, EventSearchCriteria(null, null))

    fun fetchEvents(startDate: LocalDate, endDate: LocalDate, timezone: ZoneId, criteria: EventSearchCriteria): EventsDTO

    fun createEvent(userId: UserId, command: CreateEventCommand): Boolean

    fun blockOutTimes(userId: UserId, command: BlockOutTimesCommand)
}