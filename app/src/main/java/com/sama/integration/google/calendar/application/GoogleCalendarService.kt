package com.sama.integration.google.calendar.application

import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.users.domain.UserId
import java.time.ZonedDateTime

interface GoogleCalendarService {
    fun findEvents(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): List<CalendarEvent>
    fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): CalendarEvent
}