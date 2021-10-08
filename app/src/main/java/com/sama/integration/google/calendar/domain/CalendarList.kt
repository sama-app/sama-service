package com.sama.integration.google.calendar.domain

import com.sama.users.domain.UserId
import java.time.ZoneId

data class CalendarList(
    val userId: UserId,
    val calendars: Map<GoogleCalendarId, Calendar>
) {
    val syncableCalendars = calendars.filter { (_, calendar) -> calendar.syncable }.keys

    fun merge(calendarList: CalendarList): CalendarList {
        require(userId == calendarList.userId)
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