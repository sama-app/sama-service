package com.sama.integration.google

import com.sama.connection.domain.CalendarContact
import com.sama.connection.domain.CalendarContactFinder
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentrySpan
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@SentrySpan
@Component
class GoogleCalendarContactFinder(private val googleServiceFactory: GoogleServiceFactory) : CalendarContactFinder {

    override fun scanForContacts(
        userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime
    ): Collection<CalendarContact> {
        try {
            val calendarService = googleServiceFactory.calendarService(userId)

            return calendarService.findAllEvents(startDateTime, endDateTime).first
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