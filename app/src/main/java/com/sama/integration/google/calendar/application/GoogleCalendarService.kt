package com.sama.integration.google.calendar.application

import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import com.sama.users.domain.UserId
import java.time.Instant
import java.time.ZonedDateTime

interface GoogleCalendarService {
    fun findEvents(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime? = null,
        hasAttendees: Boolean? = null
    ): List<CalendarEvent>

    fun findIdsByExtendedProperties(userId: UserId, extendedProperties: Map<String, String>): List<GoogleCalendarEventId>

    fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): CalendarEvent

    fun deleteEvent(userId: UserId, eventId: GoogleCalendarEventId)
}