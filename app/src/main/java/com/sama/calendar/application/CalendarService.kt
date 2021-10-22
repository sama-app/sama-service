package com.sama.calendar.application

import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.CalendarsDTO
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.domain.UserId

interface CalendarService {
    fun findAll(userId: UserId): CalendarsDTO

    fun addSelectedCalendar(userId: UserId, command: AddSelectedCalendarCommand): Boolean

    fun removeSelectedCalendar(userId: UserId, command: RemoveSelectedCalendarCommand): Boolean
}