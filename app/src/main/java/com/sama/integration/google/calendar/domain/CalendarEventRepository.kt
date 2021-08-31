package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarEventRepository : Repository<CalendarEvent, GoogleCalendarEventId> {
    fun find(eventKey: GoogleCalendarEventKey): CalendarEvent?
    fun findAll(userId: UserId, calendarId: GoogleCalendarId, from: ZonedDateTime, to: ZonedDateTime): List<CalendarEvent>

    fun save(event: CalendarEvent)
    fun saveAll(events: Collection<CalendarEvent>)

    fun deleteAll(eventKeys: Collection<GoogleCalendarEventKey>)
    fun deleteBy(userId: UserId, calendarId: GoogleCalendarId)
}