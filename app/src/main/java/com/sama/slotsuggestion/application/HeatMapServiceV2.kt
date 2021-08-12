package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.meeting.application.MeetingApplicationService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.BlockRepository
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.v2.HeatMap
import com.sama.slotsuggestion.domain.v2.weigher
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class HeatMapServiceV2(
    private val userRepository: UserRepository,
    private val blockRepository: BlockRepository,
    private val heatMapConfiguration: HeatMapConfiguration,
) {
    fun generate(initiatorId: UserId, recipientTimezone: ZoneId): HeatMap {
        val user = userRepository.findById(initiatorId)
        val today = LocalDate.now(user.timeZone).atStartOfDay(user.timeZone)

        val pastBlockStartDate = today.minusDays(heatMapConfiguration.historicalDays)
        val pastBlocks = blockRepository.findAllBlocksCached(initiatorId, pastBlockStartDate, today)

        val futureBlockEndDate = today.plusDays(heatMapConfiguration.futureDays)
        val futureBlocks = blockRepository.findAllBlocks(initiatorId, today, futureBlockEndDate)

        val heatMap = HeatMap.create(
            user.userId,
            user.timeZone,
            today.toLocalDate(),
            futureBlockEndDate.toLocalDate(),
            heatMapConfiguration.intervalMinutes
        )

        val weigher = weigher {
            searchBoundary()
            pastBlocks(pastBlocks)
            futureBlocks(futureBlocks)
            // futureProposedSlots(futureProposedSlots)
            workingHours(user.workingHours)
            recipientTimeZone(recipientTimezone)
            recency()
        }

        return weigher.apply(heatMap)
    }
}