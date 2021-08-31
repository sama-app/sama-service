package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import java.time.ZoneId

typealias GoogleCalendarDateTime = DateTime
typealias GoogleCalendarId = String
typealias GoogleCalendarEventId = String


data class GoogleCalendarEventsResponse(
    val events: List<GoogleCalendarEvent>, val timeZone: ZoneId, val syncToken: String?,
)

typealias GoogleCalendarEvent = Event

fun GoogleCalendarEvent.isAllDay(): Boolean {
    return start.date != null
}

fun GoogleCalendarEvent.attendeeCount(): Int {
    return attendees?.size ?: 0
}
