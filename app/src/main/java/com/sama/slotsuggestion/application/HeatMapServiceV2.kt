package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.integration.sentry.sentrySpan
import com.sama.meeting.application.MeetingDataService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.BlockRepository
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.v2.HeatMap
import com.sama.slotsuggestion.domain.v2.weigher
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class HeatMapServiceV2(
    private val userRepository: UserRepository,
    private val blockRepository: BlockRepository,
    private val meetingDataService: MeetingDataService,
    private val heatMapConfiguration: HeatMapConfiguration,
    private val clock: Clock
) {
    fun generate(initiatorId: UserId, recipientTimezone: ZoneId): HeatMap {
        val user = userRepository.findById(initiatorId)
        val today = LocalDate.now(clock.withZone(user.timeZone)).atStartOfDay(user.timeZone)

        val pastBlockStartDate = today.minusDays(heatMapConfiguration.historicalDays)
        val pastBlocks = blockRepository.findAllBlocksCached(initiatorId, pastBlockStartDate, today)

        val futureBlockEndDate = today.plusDays(heatMapConfiguration.futureDays)
        val futureBlocks = blockRepository.findAllBlocks(initiatorId, today, futureBlockEndDate)

        val futureProposedSlots = meetingDataService.findProposedSlots(initiatorId, today, futureBlockEndDate)

        sentrySpan(method = "HeatMap.create") {
            val baseHeatMap = HeatMap.create(
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
                futureProposedSlots(futureProposedSlots)
                workingHours(user.workingHours)
                recipientTimeZone(recipientTimezone)
                recency()
            }

            val heatMap = weigher.apply(baseHeatMap)
            return heatMap.normalize()
        }
    }
}