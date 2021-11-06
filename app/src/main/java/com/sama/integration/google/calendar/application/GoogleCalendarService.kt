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

    fun findEventIdsByExtendedProperties(
        userId: UserId, extendedProperties: Map<String, String>, useSamaCalendar: Boolean = false
    ): List<GoogleCalendarEventId>

    fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): Boolean
    fun deleteEvent(userId: UserId, command: DeleteGoogleCalendarEventCommand): Boolean

    fun createSamaCalendar(userId: UserId)
    fun findCalendars(userId: UserId): CalendarsDTO
    fun addSelectedCalendar(userId: UserId, command: AddSelectedCalendarCommand): Boolean
    fun removeSelectedCalendar(userId: UserId, command: RemoveSelectedCalendarCommand): Boolean
}