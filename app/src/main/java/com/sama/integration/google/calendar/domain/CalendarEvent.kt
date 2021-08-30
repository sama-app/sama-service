package com.sama.integration.google.calendar.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import org.springframework.data.repository.Repository


@DomainRepository
interface CalendarEventRepository : Repository<CalendarEvent, GoogleCalendarEventId> {
    fun find(id: GoogleCalendarEventId): CalendarEvent?
    fun findAll(
        userId: UserId,
        calendarId: GoogleCalendarId,
        from: ZonedDateTime,
        to: ZonedDateTime,
    ): List<CalendarEvent>

    fun save(event: CalendarEvent)
    fun saveAll(events: Collection<CalendarEvent>)
    fun deleteAll(eventIds: Collection<GoogleCalendarEventId>)

    fun deleteBy(userId: UserId, calendarId: GoogleCalendarId)
}


data class CalendarEvent(
    val userId: UserId,
    val calendarId: GoogleCalendarId,
    val googleEventId: GoogleCalendarEventId,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val eventData: EventData,
    val aggregatedData: AggregatedData? = null,
)

data class EventData(
    var title: String? = null,
    val allDay: Boolean,
    val attendeeCount: Int,
    var recurringEventId: GoogleCalendarEventId? = null,
)

data class AggregatedData(
    val recurrenceCount: Int,
)
