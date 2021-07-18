package com.sama.integration.google

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

typealias GoogleCalendarDateTime = DateTime
typealias GoogleCalendarEvent = Event

fun GoogleCalendarEvent.isAllDay(): Boolean {
    return this.start.date != null
}

fun ZonedDateTime.toGoogleCalendarDateTime() =
    GoogleCalendarDateTime(Date.from(this.toInstant()), TimeZone.getTimeZone(this.zone))

fun EventDateTime.toZonedDateTime(defaultZoneId: ZoneId): ZonedDateTime {
    if (this.date != null) {
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.date.value), ZoneId.of("UTC"))
        return ZonedDateTime.of(localDateTime, defaultZoneId)
    }
    if (this.dateTime != null) {
        return ZonedDateTime.parse(this.dateTime.toStringRfc3339())

    }
    throw IllegalArgumentException("invalid EventDateTime")
}
