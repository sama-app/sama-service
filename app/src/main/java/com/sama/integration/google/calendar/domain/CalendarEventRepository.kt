package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZonedDateTime
import org.springframework.data.repository.Repository

@DomainRepository
interface CalendarEventRepository : Repository<CalendarEvent, GoogleCalendarEventId> {
    fun find(eventKey: GoogleCalendarEventKey): CalendarEvent?
    fun findAll(eventKeys: Collection<GoogleCalendarEventKey>): List<CalendarEvent>
    fun findAll(accountId: GoogleAccountId, calendarId: GoogleCalendarId, from: ZonedDateTime, to: ZonedDateTime) =
        findAll(accountId, calendarId, from, to, null, null)

    fun findAll(
        accountId: GoogleAccountId,
        calendarId: GoogleCalendarId,
        from: ZonedDateTime,
        to: ZonedDateTime,
        createdFrom: ZonedDateTime?,
        minAttendeeCount: Int?,
    ): List<CalendarEvent>

    fun save(event: CalendarEvent)
    fun saveAll(events: Collection<CalendarEvent>)

    fun deleteAll(eventKeys: Collection<GoogleCalendarEventKey>)
    fun deleteBy(accountId: GoogleAccountId, calendarId: GoogleCalendarId)
}