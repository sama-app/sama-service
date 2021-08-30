package com.sama.integration.google.calendar.application

import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.users.domain.UserId
import java.time.ZonedDateTime


const val DEFAULT_CALENDAR_ID = "primary"

interface GoogleCalendarService {

    fun findEvents(
        userId: UserId,
        calendarId: GoogleCalendarId = DEFAULT_CALENDAR_ID,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
    ): List<CalendarEvent>

    fun insertEvent(
        userId: UserId,
        calendarId: GoogleCalendarId = DEFAULT_CALENDAR_ID,
        command: InsertGoogleCalendarEventCommand,
    ): CalendarEvent

    fun enableCalendarSync(userId: UserId, calendarId: GoogleCalendarId = DEFAULT_CALENDAR_ID)
}