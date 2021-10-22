package com.sama.integration.google.calendar.application

import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import com.sama.users.domain.UserId
import java.time.ZonedDateTime

interface GoogleCalendarService {
    fun findEvents(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime? = null,
        hasAttendees: Boolean? = null
    ): List<CalendarEventDTO>

    fun findIdsByExtendedProperties(userId: UserId, extendedProperties: Map<String, String>): List<GoogleCalendarEventId>

    fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): Boolean

    fun deleteEvent(userId: UserId, eventId: GoogleCalendarEventId)

    fun findCalendars(userId: UserId): CalendarsDTO

    fun addSelectedCalendar(userId: UserId, command: AddSelectedCalendarCommand): Boolean

    fun removeSelectedCalendar(userId: UserId, command: RemoveSelectedCalendarCommand): Boolean
}