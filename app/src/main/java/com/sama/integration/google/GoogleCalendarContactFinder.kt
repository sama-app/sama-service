package com.sama.integration.google

import com.sama.connection.domain.CalendarContact
import com.sama.connection.domain.CalendarContactFinder
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import org.springframework.stereotype.Component

@Component
class GoogleCalendarContactFinder : CalendarContactFinder {


    override fun scanForContacts(
        userId: UserId, dateFrom: ZonedDateTime, dateTo: ZonedDateTime
    ): Collection<CalendarContact> {
        TODO("Not yet implemented")
    }
}