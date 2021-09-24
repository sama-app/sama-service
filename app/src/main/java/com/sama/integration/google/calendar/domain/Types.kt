package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Event
import java.time.ZoneId

typealias GoogleCalendar = CalendarListEntry
typealias GoogleCalendarId = String
typealias GoogleCalendarEvent = Event
typealias GoogleCalendarEventId = String
typealias GoogleCalendarDateTime = DateTime

data class GoogleCalendarEventsResponse(
    val events: List<GoogleCalendarEvent>, val timeZone: ZoneId, val syncToken: String?,
)

data class GoogleCalendarListResponse(
    val calendar: List<GoogleCalendar>, val syncToken: String?
)

fun GoogleCalendarEvent.isAllDay(): Boolean {
    return start.date != null
}

fun GoogleCalendarEvent.attendeeCount(): Int {
    return attendees?.size ?: 0
}
