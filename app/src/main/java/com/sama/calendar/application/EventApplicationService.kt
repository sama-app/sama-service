package com.sama.calendar.application

import com.sama.calendar.domain.Event
import com.sama.calendar.domain.EventRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@ApplicationService
@Service
class EventApplicationService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) {

    fun fetchEvents(userId: UserId, startDate: LocalDate, endDate: LocalDate, timezone: ZoneId) =
        eventRepository.findAll(
            userId,
            startDate.atStartOfDay(timezone),
            endDate.plusDays(1).atStartOfDay(timezone),
        )
            .map { EventDTO(it.startDateTime, it.endDateTime, it.allDay, it.title) }
            .let { FetchEventsDTO(it, it) }


    fun createEvent(userId: UserId, command: CreateEventCommand) {
        val initiatorName = userRepository.findByIdOrThrow(userId).fullName

        val block = Event(
            command.startDateTime,
            command.endDateTime,
            false,
            // TODO: use Moustache templates
            initiatorName?.let { "Meeting with $it" },
            "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            command.recipientEmail,
        )
        eventRepository.save(userId, block)
    }
}
