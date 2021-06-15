package com.sama.api.calendar

import com.sama.api.config.AuthUserId
import com.sama.calendar.application.BlockApplicationService
import com.sama.calendar.application.FetchBlocksDTO
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
class BlockController(
    private val blockApplicationService: BlockApplicationService
) {

    @Operation(
        summary = "Retrieve user calendar blocks",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/calendar/blocks",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun fetchBlocks(
        @AuthUserId userId: UserId,
        @RequestParam @DateTimeFormat(iso = DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DATE) endDate: LocalDate,
        @RequestParam timezone: ZoneId,
    ): FetchBlocksDTO {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "'endDate' must be after 'startDate'")
        }

        return blockApplicationService.fetchBlocks(userId, startDate, endDate, timezone)
    }
}