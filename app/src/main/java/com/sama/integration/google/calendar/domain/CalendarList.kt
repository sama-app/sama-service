package com.sama.integration.google.calendar.domain

import com.google.api.services.calendar.model.CalendarListEntry
import com.sama.common.NotFoundException
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZoneId

data class CalendarList(
    val accountId: GoogleAccountId,
    val calendars: Map<GoogleCalendarId, Calendar>,
    val selected: Set<GoogleCalendarId>
) {
    companion object {
        fun new(accountId: GoogleAccountId, calendarList: List<GoogleCalendar>): CalendarList {
            val calendars = calendarList.filter { !(it.deleted ?: false) }
                .associate { it.calendarId() to it.toDomain() }
            return CalendarList(accountId, calendars, calendars.filter { it.value.selected }.keys)
        }
    }

    fun mergeFromSource(newCalendarList: List<GoogleCalendar>): CalendarList {
        val deletedCalendars = newCalendarList
            .filter { it.deleted ?: false }
            .map { it.id }
        val updatedCalendars = newCalendarList
            .filterNot { it.deleted ?: false }
            .associate { it.calendarId() to it.toDomain() }
        val newCalendars = updatedCalendars - calendars.keys

        val mergedCalendars = calendars
            .minus(deletedCalendars)
            .mapValues { (calendarId, existingCalendar) ->
                updatedCalendars[calendarId] ?: existingCalendar
            }
            .plus(newCalendars)

        val selected = selected
            .minus(deletedCalendars)
            .plus(newCalendars.filter { it.value.selected }.keys)

        return copy(calendars = mergedCalendars, selected = selected)
    }

    fun addSelection(calendarId: GoogleCalendarId): CalendarList {
        calendars[calendarId]
            ?: throw NotFoundException(Calendar::class, calendarId)

        return copy(selected = selected + calendarId)
    }

    fun removeSelection(calendarId: GoogleCalendarId): CalendarList {
        calendars[calendarId]
            ?: throw NotFoundException(Calendar::class, calendarId)

        return copy(selected = selected - calendarId)
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