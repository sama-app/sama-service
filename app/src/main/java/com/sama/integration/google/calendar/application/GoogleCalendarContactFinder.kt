package com.sama.integration.google.calendar.application

import com.sama.connection.domain.CalendarContact
import com.sama.connection.domain.CalendarContactFinder
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.translatedGoogleException
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentrySpan
import java.time.ZonedDateTime
import org.springframework.stereotype.Component

@SentrySpan
@Component
class GoogleCalendarContactFinder(private val googleServiceFactory: GoogleServiceFactory) : CalendarContactFinder {

    override fun scanForContacts(
        userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime,
    ): Collection<CalendarContact> {
        try {
            val calendarService = googleServiceFactory.calendarService(userId)

            return calendarService.findAllEvents("primary", startDateTime, endDateTime).events
                .asSequence() // more efficient
                .mapNotNull { it.attendees }
                .flatten()
                .distinct()
                .map { CalendarContact(it.email) }
                .toList()
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }
}