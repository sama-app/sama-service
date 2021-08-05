package com.sama.connection.domain

import com.sama.users.domain.UserId
import java.time.Instant
import java.time.ZonedDateTime

interface CalendarContactFinder {
    /**
     * @return scan past events of a Calendar to retrieve a list of [CalendarContact] that
     * the user had meetings with
     */
    fun scanForContacts(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ): Collection<CalendarContact>
}

data class CalendarContact(val email: String)