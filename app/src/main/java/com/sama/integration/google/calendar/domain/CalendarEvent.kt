package com.sama.integration.google.calendar.domain

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZoneId
import java.time.ZonedDateTime

data class GoogleCalendarEventKey(
    val accountId: GoogleAccountId,
    val calendarId: GoogleCalendarId,
    val eventId: GoogleCalendarEventId,
)

data class CalendarEvent(
    val key: GoogleCalendarEventKey,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val eventData: EventData,
    val labels: Set<EventLabel>
)

data class EventData(
    val title: String? = null,
    val allDay: Boolean,
    val attendeeCount: Int,
    val recurringEventId: GoogleCalendarEventId? = null,
    val created: ZonedDateTime? = null,
    val privateExtendedProperties: Map<String, String> = emptyMap()
)

data class AggregatedData(
    val recurrenceCount: Int,
)

enum class EventLabel {
    SELF_BLOCK,
    ONE_ON_ONE,
    EXTERNAL,
    TEAM,
    RECURRING;

    companion object {
        fun labelsOf(event: GoogleCalendarEvent, googleAccount: GoogleAccount): Set<EventLabel> {
            val result = mutableSetOf<EventLabel>()

            val attendeeCount = event.attendeeCount()
            when {
                attendeeCount == 0 -> result.add(SELF_BLOCK)
                attendeeCount == 1 -> {
                    val attendee = event.attendees[0]
                    val attendeeEmailDomain = attendee.email.split('@')[1]
                    if (attendee.email == googleAccount.email) {
                        result.add(SELF_BLOCK)
                    } else if (attendeeEmailDomain == googleAccount.domain) {
                        result.add(ONE_ON_ONE)
                    }
                }
                attendeeCount > 1 -> {
                    val allSameDomain = event.attendees.map { it.domain() }
                        .all { it == googleAccount.domain }
                    if (allSameDomain) {
                        result.add(TEAM)
                    }
                }
            }

            val hasDifferentDomain = event.attendees?.map { it.domain() }
                ?.find { it != googleAccount.domain } != null
            if (hasDifferentDomain) {
                result.add(EXTERNAL)
            }

            if (event.recurringEventId != null) {
                result.add(RECURRING)
            }

            return result
        }
    }
}


typealias GoogleCalendarEvent = Event
typealias GoogleCalendarEventId = String
typealias GoogleCalendarDateTime = DateTime

data class GoogleCalendarEventsResponse(
    val events: List<GoogleCalendarEvent>, val timeZone: ZoneId, val syncToken: String?,
)

val ACCEPTED_EVENT_STATUSES = listOf("confirmed", "tentative")

fun Collection<GoogleCalendarEvent>.toDomain(account: GoogleAccount, calendarId: GoogleCalendarId, timeZone: ZoneId) =
    filter { it.status in ACCEPTED_EVENT_STATUSES }
        .map { it.toDomain(account, calendarId, timeZone) }


fun GoogleCalendarEvent.toDomain(account: GoogleAccount, calendarId: GoogleCalendarId, timeZone: ZoneId): CalendarEvent {
    return CalendarEvent(
        toKey(account.id!!, calendarId),
        start.toZonedDateTime(timeZone),
        end.toZonedDateTime(timeZone),
        EventData(
            summary,
            isAllDay(),
            attendeeCount(),
            recurringEventId,
            created?.toZonedDateTime(),
            extendedProperties?.private ?: emptyMap()
        ),
        EventLabel.labelsOf(this, account)
    )
}

fun GoogleCalendarEvent.toKey(accountId: GoogleAccountId, calendarId: GoogleCalendarId): GoogleCalendarEventKey {
    return GoogleCalendarEventKey(accountId, calendarId, id)
}

fun GoogleCalendarEvent.isAllDay(): Boolean {
    return start.date != null
}

fun GoogleCalendarEvent.attendeeCount(): Int {
    return attendees?.size ?: 0
}

fun EventAttendee.domain(): String? {
    return email?.split('@')?.get(1)
}

fun Collection<GoogleCalendarEvent>.attendeeEmails(): Set<String> {
    return asSequence() // more efficient
        .mapNotNull { it.attendees }
        .flatten()
        .distinct()
        .map { it.email }
        .toSet()
}
