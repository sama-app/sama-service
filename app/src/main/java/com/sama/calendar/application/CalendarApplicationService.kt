package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.CalendarsDTO
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class CalendarApplicationService(private val googleCalendarService: GoogleCalendarService) : CalendarService {

    override fun findAll(userId: UserId): CalendarsDTO {
        return googleCalendarService.findCalendars(userId)
    }

    override fun addSelectedCalendar(userId: UserId, command: AddSelectedCalendarCommand): Boolean {
        return googleCalendarService.addSelectedCalendar(userId, command)
    }

    override fun removeSelectedCalendar(userId: UserId, command: RemoveSelectedCalendarCommand): Boolean {
        return googleCalendarService.removeSelectedCalendar(userId, command)
    }
}