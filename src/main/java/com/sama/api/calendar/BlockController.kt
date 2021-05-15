package com.sama.api.calendar

import com.sama.api.common.UserId
import com.sama.calendar.application.BlockApplicationService
import com.sama.calendar.application.FetchBlocksResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate


@RestController
class BlockController(
    private val blockApplicationService: BlockApplicationService
) {

    @GetMapping("/api/calendar/blocks")
    fun fetchBlocks(
        @UserId userId: Long,
        @RequestParam @DateTimeFormat(iso = DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DATE) endDate: LocalDate
    ): FetchBlocksResponse {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "'endDate' must be after 'startDate'")
        }

        return blockApplicationService.fetchBlocks(userId, startDate, endDate)
    }
}