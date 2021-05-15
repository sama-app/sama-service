package com.sama.calendar.application

import com.sama.calendar.domain.BlockRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class BlockApplicationService(
    private val blockRepository: BlockRepository
) {

    fun fetchBlocks(userId: Long, startDate: LocalDate, endDate: LocalDate): FetchBlocksResponse {
        val blocks = blockRepository.findAll(
            userId,
            startDate.atStartOfDay(ZoneId.systemDefault()),
            endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault())
        ).map {
            BlockResponse(it.startDateTime, it.endDateTime, it.allDay, it.title)
        }
        return FetchBlocksResponse(blocks)
    }
}

data class BlockResponse(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val allDay: Boolean,
    val title: String?
)

data class FetchBlocksResponse(
    val blocks: List<BlockResponse>
)