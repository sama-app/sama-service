package com.sama.integration.google.calendar.domain

import com.google.api.services.calendar.model.CalendarListEntry
import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.ZoneId

data class CalendarList(
    val accountId: GoogleAccountId,
    val calendars: Map<GoogleCalendarId, Calendar>
) {
    val syncableCalendars = calendars.filter { (_, calendar) -> calendar.syncable }.keys

    fun merge(calendarList: CalendarList): CalendarList {
        require(accountId == calendarList.accountId)
        return copy(calendars = calendars + calendarList.calendars)
    }
}

data class Calendar(
    val timeZone: ZoneId?,
    val selected: Boolean,
    val isOwner: Boolean
) {
    val syncable = selected && isOwner
}


typealias GoogleCalendar = CalendarListEntry
typealias GoogleCalendarId = String

data class GoogleCalendarListResponse(
    val calendars: List<GoogleCalendar>, val syncToken: String?
)

fun Collection<GoogleCalendar>.toDomain(accountId: GoogleAccountId): CalendarList {
    val calendars = associate { it.calendarId() to it.toDomain() }
    return CalendarList(accountId, calendars)
}

fun GoogleCalendar.toDomain(): Calendar {
    val isOwner = accessRole == "owner"
    val primary = primary ?: false
    return Calendar(
        timeZone?.let { ZoneId.of(it) },
        selected ?: primary,
        isOwner
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