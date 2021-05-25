package com.sama.calendar.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@ApplicationService
@Service
class BlockApplicationService(
    private val blockRepository: BlockRepository
) {

    fun fetchBlocks(userId: UserId, startDate: LocalDate, endDate: LocalDate) =
        blockRepository.findAll(
            userId,
            startDate.atStartOfDay(ZoneId.systemDefault()),
            endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault())
        )
            .map { BlockDTO(it.startDateTime, it.endDateTime, it.allDay, it.title) }
            .let { FetchBlocksDTO(it) }
}
