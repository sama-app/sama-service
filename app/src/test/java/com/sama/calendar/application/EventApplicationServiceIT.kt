package com.sama.calendar.application

import com.sama.common.BaseApplicationIntegrationTest
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.application.CalendarEventDTO
import com.sama.integration.google.calendar.application.EventAttendee
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarEventKey
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingCode
import com.sama.users.application.InternalUserService
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
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
        val userId = initiator().id
        val startDate = LocalDate.now(clock)
        val endDate = LocalDate.now(clock).plusDays(7)

        val startDateTime = startDate.atStartOfDay(clock.zone)
        val endDateTime = endDate.plusDays(1).atStartOfDay(clock.zone)
        val accountId = GoogleAccountPublicId(UUID.randomUUID())
        whenever(googleCalendarService.findEvents(userId, startDateTime, endDateTime))
            .thenReturn(
                listOf(
                    CalendarEventDTO(
                        accountId, "primary", "id#1",
                        startDateTime, startDateTime.plusHours(1),
                        EventData("title", false, 3),
                        AggregatedData(0)
                    )
                )
            )

        val result = asInitiator {
            underTest.fetchEvents(startDate, endDate, clock.zone)
        }

        val expected = EventDTO(startDateTime, startDateTime.plusHours(1), false, "title", accountId, "primary", "id#1", false)
        assertThat(result).isEqualTo(EventsDTO(listOf(expected)))
    }

    @Test
    fun `create event with e-mail recipient`() {
        val user = initiator()
        whenever(internalUserService.findInternal(user.id)).thenReturn(user)
        val recipientEmail = recipient().email

        val meetingCode = MeetingCode("code")
        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)

        whenever(
            googleCalendarService.insertEvent(
                user.id,
                InsertGoogleCalendarEventCommand(
                    startDateTime, endDateTime, "title",
                    "Time for this meeting was created via <a href=http://localhost:8080>Sama app</a>",
                    listOf(EventAttendee(initiator().email), EventAttendee((recipientEmail))),
                )
            )
        ).thenReturn(true)

        val actual = underTest.createEvent(
            user.id,
            CreateEventCommand(meetingCode, startDateTime, endDateTime, EmailRecipient.of(recipientEmail), "title")
        )

        assertThat(actual).isTrue()
    }

    @Test
    fun `create event with sama recipient`() {
        val initiator = initiator()
        val recipient = recipient()
        whenever(internalUserService.findInternal(initiator.id)).thenReturn(initiator)
        whenever(internalUserService.findInternal(recipient.id)).thenReturn(recipient)

        val meetingCode = MeetingCode("code")
        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)

        whenever(
            googleCalendarService.insertEvent(
                initiator.id,
                InsertGoogleCalendarEventCommand(
                    startDateTime, endDateTime, "title",
                    "Time for this meeting was created via <a href=http://localhost:8080>Sama app</a>",
                    listOf(EventAttendee(initiator().email), EventAttendee((recipient().email))),
                )
            )
        ).thenReturn(true)

        val actual = underTest.createEvent(
            initiator.id,
            CreateEventCommand(meetingCode, startDateTime, endDateTime, EmailRecipient.of(recipient().email), "title")
        )

        assertThat(actual).isTrue()
    }

    @Test
    fun `create event with blocked out times`() {
        val user = initiator()
        whenever(internalUserService.findInternal(user.id)).thenReturn(user)
        val recipientEmail = recipient().email

        val meetingCode = MeetingCode("code")
        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)
        val extendedProperties = mapOf(
            "meeting_code" to meetingCode.code,
            "event_type" to "meeting_block"
        )

        whenever(
            googleCalendarService.insertEvent(
                user.id,
                InsertGoogleCalendarEventCommand(
                    startDateTime, endDateTime, "title",
                    "Time for this meeting was created via <a href=http://localhost:8080>Sama app</a>",
                    listOf(EventAttendee(initiator().email), EventAttendee((recipientEmail))),
                )
            )
        ).thenReturn(true)

        val blockedOutEventIds = listOf("event_id_1")
        whenever(googleCalendarService.findIdsByExtendedProperties(user.id, extendedProperties))
            .thenReturn(blockedOutEventIds)

        val actual = underTest.createEvent(
            user.id,
            CreateEventCommand(meetingCode, startDateTime, endDateTime, EmailRecipient.of(recipientEmail), "title")
        )

        assertThat(actual).isTrue()

        verify(googleCalendarService).deleteEvent(user.id, blockedOutEventIds[0])
    }


    @Test
    fun `block out slots`() {
        val initiatorId = initiator().id
        val meetingCode = MeetingCode("code")

        val startDateTime = ZonedDateTime.now(clock)
        val endDateTime = startDateTime.plusHours(1)

        underTest.blockOutTimes(
            initiatorId,
            BlockOutTimesCommand(meetingCode, "title", slots = listOf(Slot(startDateTime, endDateTime)))
        )

        verify(googleCalendarService)
            .insertEvent(
                initiatorId,
                InsertGoogleCalendarEventCommand(
                    startDateTime,
                    endDateTime,
                    "Reserved by Sama",
                    description = """
                        Blocked for title
                        
                        Meeting link: http://localhost:3000/code
                    """.trimIndent(),
                    attendees = emptyList(),
                    conferenceType = null,
                    privateExtendedProperties = mapOf(
                        "event_type" to "meeting_block",
                        "meeting_code" to meetingCode.code
                    )
                )
            )
    }
}