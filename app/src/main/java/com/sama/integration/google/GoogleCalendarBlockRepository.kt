package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.SamaApplication
import com.sama.calendar.domain.Block
import com.sama.calendar.domain.BlockRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Component
class GoogleCalendarBlockRepository(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleNetHttpTransport: HttpTransport,
    private val googleJacksonFactory: JacksonFactory
) : BlockRepository {
    private val defaultCalendarTimeZone = ZoneId.of("UTC")

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findAll(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): List<Block> {
        val calendarService = calendarServiceForUser(userId)

        var nextPageToken: String? = null
        val blocks = mutableListOf<Block>()
        do {
            val result = calendarService.list(startDateTime, endDateTime, nextPageToken).execute()
            val calendarTimeZone = result.timeZone?.let { ZoneId.of(it) } ?: defaultCalendarTimeZone
            nextPageToken = result.nextPageToken

            blocks.addAll(result.items
                .filter { it.status in listOf("confirmed", "tentative") }
                .map { it.toBlock(calendarTimeZone) }
            )

        } while (nextPageToken != null)

        return blocks
    }

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun save(userId: UserId, block: Block): Block {
        val calendarService = calendarServiceForUser(userId)
        val timeZone = block.startDateTime.zone
        val event = Event().apply {
            start = EventDateTime()
                .setDateTime(block.startDateTime.toGoogleCalendarDateTime())
                .setTimeZone(timeZone.id)
            end = EventDateTime()
                .setDateTime(block.endDateTime.toGoogleCalendarDateTime())
                .setTimeZone(timeZone.id)
            attendees = listOf(EventAttendee().apply {
                email = block.recipientEmail
            })
        }

        val inserted = calendarService.events()
            .insert("primary", event)
            .setSendNotifications(true)
            .execute()
        return inserted.toBlock(timeZone)
    }

    private fun calendarServiceForUser(userId: Long): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Calendar.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    private fun Calendar.list(
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        nextPageToken: String?
    ): Calendar.Events.List {
        val requestBuilder = this.events().list("primary")
            .setMaxResults(2500)
            .setTimeMin(startDateTime.toGoogleCalendarDateTime())
            .setTimeMax(endDateTime.toGoogleCalendarDateTime())
            .setOrderBy("startTime")
            .setSingleEvents(true)
        nextPageToken?.run { requestBuilder.setPageToken(this) }
        return requestBuilder
    }

    private fun ZonedDateTime.toGoogleCalendarDateTime() =
        DateTime(Date.from(this.toInstant()), TimeZone.getTimeZone(this.zone))

    private fun EventDateTime.toZonedDateTime(defaultZoneId: ZoneId): ZonedDateTime {
        if (this.date != null) {
            val localDateTime = ofInstant(Instant.ofEpochMilli(this.date.value), ZoneId.of("UTC"))
            return ZonedDateTime.of(localDateTime, defaultZoneId)
        }
        if (this.dateTime != null) {
            return ZonedDateTime.parse(this.dateTime.toStringRfc3339())

        }
        throw IllegalArgumentException("invalid EventDateTime")
    }

    private fun Event.isAllDay(): Boolean {
        return this.start.date != null
    }

    private fun Event.toBlock(calendarTimeZone: ZoneId): Block {
        return Block(
            this.start.toZonedDateTime(calendarTimeZone),
            this.end.toZonedDateTime(calendarTimeZone),
            this.isAllDay(),
            this.summary,
            if (this.attendees != null) {
                this.attendees.firstOrNull()?.email
            } else {
                null
            }
        )
    }
}