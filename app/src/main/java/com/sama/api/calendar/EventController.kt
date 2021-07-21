package com.sama.api.calendar

import com.sama.api.config.AuthUserId
import com.sama.calendar.application.EventApplicationService
import com.sama.calendar.application.FetchEventsDTO
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneId

@Tag(name = "calendar")
@RestController
class EventController(
    private val eventApplicationService: EventApplicationService
) {

    @Operation(
        summary = "Retrieve user calendar blocks",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        value = ["/api/calendar/blocks", "/api/calendar/events"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun fetchEvents(
        @AuthUserId userId: UserId,
        @RequestParam @DateTimeFormat(iso = DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DATE) endDate: LocalDate,
        @RequestParam timezone: ZoneId,
    ): FetchEventsDTO {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "'endDate' must be after 'startDate'")
        }

        return eventApplicationService.fetchBlocks(userId, startDate, endDate, timezone)
    }
}