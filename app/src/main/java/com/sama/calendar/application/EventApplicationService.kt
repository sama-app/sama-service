package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.UserRecipient
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@ApplicationService
@Service
class EventApplicationService(
    private val googleCalendarService: GoogleCalendarService,
    private val internalUserService: InternalUserService,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) : EventService {

    override fun fetchEvents(
        userId: UserId,
        startDate: LocalDate,
        endDate: LocalDate,
        timezone: ZoneId,
        searchCriteria: EventSearchCriteria
    ) = googleCalendarService.findEvents(
        userId = userId,
        startDateTime = startDate.atStartOfDay(timezone),
        endDateTime = endDate.plusDays(1).atStartOfDay(timezone),
        createdFrom = searchCriteria.createdFrom,
        hasAttendees = searchCriteria.hasAttendees
    )
        .map { EventDTO(it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title) }
        .let { FetchEventsDTO(it) }

    override fun createEvent(userId: UserId, command: CreateEventCommand): EventDTO {
        val initiator = internalUserService.findInternal(userId)
        val initiatorEmail = initiator.email
        val timeZone = initiator.settings.timeZone
        val recipientEmail = when (val recipient = command.recipient) {
            is EmailRecipient -> recipient.email
            is UserRecipient -> internalUserService.find(recipient.recipientId).email
        }

        val insertCommand = InsertGoogleCalendarEventCommand(
            command.startDateTime.withZoneSameInstant(timeZone),
            command.endDateTime.withZoneSameInstant(timeZone),
            command.title,
            "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            initiatorEmail,
            recipientEmail
        )
        return googleCalendarService.insertEvent(userId = userId, command = insertCommand)
            .let { EventDTO(it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title) }
    }
}
