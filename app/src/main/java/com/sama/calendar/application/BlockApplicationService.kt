package com.sama.calendar.application

import com.sama.calendar.domain.Block
import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@ApplicationService
@Service
class BlockApplicationService(private val blockRepository: BlockRepository) {

    fun fetchBlocks(userId: UserId, startDate: LocalDate, endDate: LocalDate, timezone: ZoneId) =
        blockRepository.findAll(
            userId,
            startDate.atStartOfDay(timezone),
            endDate.plusDays(1).atStartOfDay(timezone),
        )
            .map { BlockDTO(it.startDateTime, it.endDateTime, it.allDay, it.title) }
            .let { FetchBlocksDTO(it) }


    fun createBlock(userId: UserId, command: CreateBlockCommand) {
        val block = Block(command.startDateTime, command.endDateTime, false, null, command.recipientEmail)
        blockRepository.save(userId, block)
    }
}
