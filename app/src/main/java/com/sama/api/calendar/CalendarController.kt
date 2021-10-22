package com.sama.api.calendar

import com.sama.api.config.AuthUserId
import com.sama.calendar.application.CalendarService
import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "calendar")
@RestController
class CalendarController(private val calendarService: CalendarService) {

    @Operation(
        summary = "Retrieve users calendars",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        value = ["/api/calendar/calendars"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun fetchCalendars(@AuthUserId userId: UserId?) = calendarService.findAll(userId!!)

    @Operation(
        summary = "Add a selected calendar",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(value = ["/api/calendar/calendars/add"])
    fun addSelectedCalendar(@AuthUserId userId: UserId?, @RequestBody command: AddSelectedCalendarCommand) =
        calendarService.addSelectedCalendar(userId!!, command)

    @Operation(
        summary = "Remove a selected calendar",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(value = ["/api/calendar/calendars/remove"])
    fun removeSelectedCalendar(@AuthUserId userId: UserId?, @RequestBody command: RemoveSelectedCalendarCommand) =
        calendarService.removeSelectedCalendar(userId!!, command)
}