package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.CalendarsDTO
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.application.AuthUserService
import org.springframework.stereotype.Service

@ApplicationService
@Service
class CalendarApplicationService(
    private val googleCalendarService: GoogleCalendarService,
    private val authUserService: AuthUserService
) : CalendarService {

    override fun findAll(): CalendarsDTO {
        val userId = authUserService.currentUserId()
        return googleCalendarService.findCalendars(userId)
    }

    override fun addSelectedCalendar(command: AddSelectedCalendarCommand): Boolean {
        val userId = authUserService.currentUserId()
        return googleCalendarService.addSelectedCalendar(userId, command)
    }

    override fun removeSelectedCalendar(command: RemoveSelectedCalendarCommand): Boolean {
        val userId = authUserService.currentUserId()
        return googleCalendarService.removeSelectedCalendar(userId, command)
    }
}