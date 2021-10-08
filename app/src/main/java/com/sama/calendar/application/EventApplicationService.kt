package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.integration.google.calendar.application.SyncGoogleCalendarService
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.UserRecipient
import com.sama.users.application.InternalUserService
import com.sama.users.application.UserService
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import liquibase.pro.packaged.it
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@ApplicationService
@Service
class EventApplicationService(
    private val googleCalendarService: SyncGoogleCalendarService,
    private val internalUserService: InternalUserService,
    private val userSettingsService: UserSettingsService,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) {

    fun fetchEvents(userId: UserId, startDate: LocalDate, endDate: LocalDate, timezone: ZoneId) =
        googleCalendarService.findEvents(
            userId = userId,
            startDateTime = startDate.atStartOfDay(timezone),
            endDateTime = endDate.plusDays(1).atStartOfDay(timezone),
        )
            .map { EventDTO(it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title) }
            .let { FetchEventsDTO(it, it) }


    fun createEvent(userId: UserId, command: CreateEventCommand): EventDTO {
        val initiatorEmail = internalUserService.find(userId).email
        val timeZone = userSettingsService.find(userId).timeZone
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
