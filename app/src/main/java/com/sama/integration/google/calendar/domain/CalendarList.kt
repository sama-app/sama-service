package com.sama.integration.google.calendar.domain

import com.google.api.services.calendar.model.CalendarListEntry
import com.sama.common.NotFoundException
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZoneId

data class CalendarList(
    val accountId: GoogleAccountId,
    val calendars: Map<GoogleCalendarId, Calendar>
) {
    val syncableCalendars = calendars.filter { (_, calendar) -> calendar.selected }.keys

    fun mergeFromSource(newCalendarList: List<GoogleCalendar>): CalendarList {
        val deletedCalendars = newCalendarList.filter { it.deleted ?: false }.map { it.id }
        val updatedCalendars = newCalendarList.toDomain(accountId)
        val newCalendars = updatedCalendars.calendars - calendars.keys

        val mergedCalendars = calendars
            .minus(deletedCalendars)
            .mapValues { (calendarId, existingCalendar) ->
                updatedCalendars.calendars[calendarId]
                    ?.copy(selected = existingCalendar.selected)
                    ?: existingCalendar
            }
            .plus(newCalendars)

        return copy(calendars = mergedCalendars)
    }

    fun addSelection(calendarId: GoogleCalendarId): CalendarList {
        val updated = calendars[calendarId]
            ?.copy(selected = true)
            ?: throw NotFoundException(Calendar::class, calendarId)
        return copy(calendars = calendars + (calendarId to updated))
    }

    fun removeSelection(calendarId: GoogleCalendarId): CalendarList {
        val updated = calendars[calendarId]
            ?.copy(selected = false)
            ?: throw NotFoundException(Calendar::class, calendarId)
        return copy(calendars = calendars + (calendarId to updated))
    }
}

data class Calendar(
    val timeZone: ZoneId?,
    val primary: Boolean,
    val selected: Boolean,
    val summary: String?,
    val backgroundColor: String?,
    val foregroundColor: String?
)

typealias GoogleCalendar = CalendarListEntry
typealias GoogleCalendarId = String

data class GoogleCalendarListResponse(
    val calendars: List<GoogleCalendar>, val syncToken: String?
)

fun Collection<GoogleCalendar>.toDomain(accountId: GoogleAccountId): CalendarList {
    val calendars = filter { !(it.deleted ?: false) }
        .associate { it.calendarId() to it.toDomain() }
    return CalendarList(accountId, calendars)
}

fun GoogleCalendar.toDomain(): Calendar {
    val isOwner = accessRole == "owner"
    val primary = primary ?: false
    return Calendar(
        timeZone?.let { ZoneId.of(it) },
        primary,
        (primary || (selected ?: false)) && isOwner,
        summaryOverride ?: summary,
        backgroundColor,
        foregroundColor
    )
}

/**
 * Google Calendar defined constant for the primary calendar
 */
const val PRIMARY_CALENDAR_ID = "primary"
fun GoogleCalendar.calendarId(): String {
    val primary = primary ?: false
    return if (primary) PRIMARY_CALENDAR_ID else id
}