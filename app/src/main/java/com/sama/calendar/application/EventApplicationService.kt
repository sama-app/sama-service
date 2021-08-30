package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.integration.google.calendar.application.SyncGoogleCalendarService
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@ApplicationService
@Service
class EventApplicationService(
    private val googleCalendarService: SyncGoogleCalendarService,
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
        val insertCommand = InsertGoogleCalendarEventCommand(
            command.startDateTime, command.endDateTime, command.title,
            "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            command.recipientEmail
        )
        return googleCalendarService.insertEvent(userId = userId, command = insertCommand)
            .let { EventDTO(it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title) }
    }
}
