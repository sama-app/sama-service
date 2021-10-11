package com.sama.calendar.application

import com.sama.common.BaseApplicationIntegrationTest
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarEventKey
import com.sama.meeting.domain.EmailRecipient
import com.sama.users.application.InternalUserService
import com.sama.users.application.UserSettingsDTO
import com.sama.users.application.toInternalDTO
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class EventApplicationServiceIT : BaseApplicationIntegrationTest() {

    @Autowired
    lateinit var underTest: EventApplicationService

    @Autowired
    lateinit var clock: Clock

    @MockBean
    lateinit var googleCalendarService: GoogleCalendarService

    @MockBean
    lateinit var internalUserService: InternalUserService

    @Test
    fun `fetch events`() {
        val userId = initiator().id!!
        val startDate = LocalDate.now(clock)
        val endDate = LocalDate.now(clock).plusDays(7)

        val startDateTime = startDate.atStartOfDay(clock.zone)
        val endDateTime = endDate.plusDays(1).atStartOfDay(clock.zone)
        whenever(googleCalendarService.findEvents(userId, startDateTime, endDateTime))
            .thenReturn(
                listOf(
                    CalendarEvent(
                        GoogleCalendarEventKey(GoogleAccountId(1L), "primary", "id#1"),
                        startDateTime, startDateTime.plusHours(1),
                        EventData("title", false, 3)
                    )
                )
            )

        val result = asInitiator {
            underTest.fetchEvents(userId, startDate, endDate, clock.zone)
        }

        val expected = EventDTO(startDateTime, startDateTime.plusHours(1), false, "title")
        assertThat(result).isEqualTo(FetchEventsDTO(blocks = listOf(expected), events = listOf(expected)))
    }

    @Test
    fun `create event with e-mail recipient`() {
        val userId = initiator().id!!

        val userSettings = UserSettingsDTO(Locale.ENGLISH, clock.zone, true, emptyList(), emptySet())
        val user = initiator().toInternalDTO(userSettings)
        whenever(internalUserService.findInternal(userId)).thenReturn(user)
        val recipientEmail = recipient().email

        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)

        val createdEvent = CalendarEvent(
            GoogleCalendarEventKey(GoogleAccountId(1L), "primary", "id#1"),
            startDateTime, startDateTime.plusHours(1),
            EventData("title", false, 3)
        )
        whenever(
            googleCalendarService.insertEvent(
                userId,
                InsertGoogleCalendarEventCommand(
                    startDateTime, endDateTime, "title",
                    "Time for this meeting was created via <a href=http://localhost:8080>Sama app</a>",
                    initiator().email, recipientEmail
                )
            )
        ).thenReturn(createdEvent)

        val actual = underTest.createEvent(
            userId,
            CreateEventCommand(startDateTime, endDateTime, EmailRecipient.of(recipientEmail), "title")
        )

        assertThat(actual).isEqualTo(
            EventDTO(createdEvent.startDateTime, createdEvent.endDateTime, false, "title")
        )
    }

    @Test
    fun `create event with sama recipient`() {
        val initiatorId = initiator().id!!
        val recipientId = recipient().id!!

        val userSettings = UserSettingsDTO(Locale.ENGLISH, clock.zone, true, emptyList(), emptySet())
        val initiator = initiator().toInternalDTO(userSettings)
        val recipient = recipient().toInternalDTO(userSettings)
        whenever(internalUserService.findInternal(initiatorId)).thenReturn(initiator)
        whenever(internalUserService.findInternal(recipientId)).thenReturn(recipient)


        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)

        val createdEvent = CalendarEvent(
            GoogleCalendarEventKey(GoogleAccountId(1L), "primary", "id#1"),
            startDateTime, startDateTime.plusHours(1),
            EventData("title", false, 3)
        )
        whenever(
            googleCalendarService.insertEvent(
                initiatorId,
                InsertGoogleCalendarEventCommand(
                    startDateTime, endDateTime, "title",
                    "Time for this meeting was created via <a href=http://localhost:8080>Sama app</a>",
                    initiator().email, recipient().email
                )
            )
        ).thenReturn(createdEvent)

        val actual = underTest.createEvent(
            initiatorId,
            CreateEventCommand(startDateTime, endDateTime, EmailRecipient.of(recipient().email), "title")
        )

        assertThat(actual).isEqualTo(
            EventDTO(createdEvent.startDateTime, createdEvent.endDateTime, false, "title")
        )
    }
}