package com.sama.integration.google.calendar.application

import java.time.ZonedDateTime

data class InsertGoogleCalendarEventCommand(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val title: String,
    val description: String?,
    val attendees: List<EventAttendee>,
    val conferenceType: ConferenceType? = ConferenceType.GOOGLE_MEET,
    val privateExtendedProperties: Map<String, String> = emptyMap()
)

data class EventAttendee(val email: String)

enum class ConferenceType {
    GOOGLE_MEET
}