package com.sama.integration.google.calendar.application

import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.domain.GoogleCalendarId
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

data class AddSelectedCalendarCommand(val accountId: GoogleAccountPublicId, val calendarId: GoogleCalendarId)
data class RemoveSelectedCalendarCommand(val accountId: GoogleAccountPublicId, val calendarId: GoogleCalendarId)