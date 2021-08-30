package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.TimeZone

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
