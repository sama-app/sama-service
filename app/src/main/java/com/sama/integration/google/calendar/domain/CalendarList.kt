package com.sama.integration.google.calendar.domain

import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.users.domain.UserId
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
    val timeZone: ZoneId,
    val selected: Boolean,
    val isOwner: Boolean
) {
    val syncable = selected && isOwner
}