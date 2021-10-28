package com.sama.calendar.application

import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.CalendarsDTO
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.domain.UserId

interface CalendarService {
    fun findAll(): CalendarsDTO
    fun addSelectedCalendar(command: AddSelectedCalendarCommand): Boolean
    fun removeSelectedCalendar(command: RemoveSelectedCalendarCommand): Boolean
}