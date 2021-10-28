package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.EventAttendee
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.UserRecipient
import com.sama.users.application.AuthUserService
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private const val MEETING_CODE_PROPERTY_KEY = "meeting_code"

@ApplicationService
@Service
class EventApplicationService(
    private val googleCalendarService: GoogleCalendarService,
    private val internalUserService: InternalUserService,
    private val authUserService: AuthUserService,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) : EventService {

    override fun fetchEvents(
        startDate: LocalDate, endDate: LocalDate, timezone: ZoneId, criteria: EventSearchCriteria
    ): EventsDTO {
        val userId = authUserService.currentUserId()
        val events = googleCalendarService.findEvents(
            userId = userId,
            startDateTime = startDate.atStartOfDay(timezone),
            endDateTime = endDate.plusDays(1).atStartOfDay(timezone),
            createdFrom = criteria.createdFrom,
            hasAttendees = criteria.hasAttendees
        )
        return events
            .map {
                EventDTO(
                    it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title,
                    it.accountId, it.calendarId, it.eventId
                )
            }
            .let { EventsDTO(it) }
    }

    override fun createEvent(userId: UserId, command: CreateEventCommand): Boolean {
        val initiator = internalUserService.findInternal(userId)
        val initiatorEmail = initiator.email
        val timeZone = initiator.settings.timeZone
        val recipientEmail = when (val recipient = command.recipient) {
            is EmailRecipient -> recipient.email
            is UserRecipient -> internalUserService.find(recipient.recipientId).email
        }

        val extendedProperties = mapOf(MEETING_CODE_PROPERTY_KEY to command.meetingCode.code)

        val blockedOutTimesEventIds = googleCalendarService.findIdsByExtendedProperties(userId, extendedProperties)

        val insertCommand = InsertGoogleCalendarEventCommand(
            command.startDateTime.withZoneSameInstant(timeZone),
            command.endDateTime.withZoneSameInstant(timeZone),
            command.title,
            description = "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            listOf(EventAttendee(initiatorEmail), EventAttendee(recipientEmail)),
        )

        runBlocking(Dispatchers.IO) {
            val insertEvent = async {
                googleCalendarService.insertEvent(userId = userId, command = insertCommand)
            }

            blockedOutTimesEventIds.forEach {
                launch { googleCalendarService.deleteEvent(userId, it) }
            }

            insertEvent.await()
        }

        return true
    }

    override fun blockOutTimes(userId: UserId, command: BlockOutTimesCommand) {
        if (command.slots.isEmpty()) {
            return
        }

        val meetingCode = command.meetingCode.code

        val commands = command.slots
            .map {
                InsertGoogleCalendarEventCommand(
                    it.startDateTime,
                    it.endDateTime,
                    "Blocked for ${command.meetingTitle}",
                    description = null,
                    attendees = emptyList(),
                    conferenceType = null,
                    privateExtendedProperties = mapOf(MEETING_CODE_PROPERTY_KEY to meetingCode)
                )
            }

        runBlocking(Dispatchers.IO) {
            commands.forEach { launch { googleCalendarService.insertEvent(userId, it) } }
        }
    }
}
