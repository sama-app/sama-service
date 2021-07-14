package com.sama.calendar.application

import com.sama.calendar.domain.Block
import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.ZoneId

@ApplicationService
@Service
class BlockApplicationService(
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) {

    fun fetchBlocks(userId: UserId, startDate: LocalDate, endDate: LocalDate, timezone: ZoneId) =
        blockRepository.findAll(
            userId,
            startDate.atStartOfDay(timezone),
            endDate.plusDays(1).atStartOfDay(timezone),
        )
            .map { BlockDTO(it.startDateTime, it.endDateTime, it.allDay, it.title) }
            .let { FetchBlocksDTO(it) }


    fun createBlock(userId: UserId, command: CreateBlockCommand) {
        val initiatorName = userRepository.findByIdOrThrow(userId).fullName

        val block = Block(
            command.startDateTime,
            command.endDateTime,
            false,
            // TODO: use Moustache templates
            initiatorName?.let { "Meeting with $it" },
            "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            command.recipientEmail,
            1,
            null
        )
        blockRepository.save(userId, block)
    }
}
