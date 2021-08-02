package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.*
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@ApplicationService
@Service
class HeatMapService(
    private val userRepository: UserRepository,
    private val blockRepository: BlockRepository,
    private val heatMapConfiguration: HeatMapConfiguration,
    private val weightContext: WeightContext
) {
    fun generate(initiatorId: UserId, recipientTimezone: ZoneId): HeatMap {
        val user = userRepository.findById(initiatorId)
        val today = LocalDate.now(user.timeZone).atStartOfDay(user.timeZone)

        val pastBlockStartDate = today.minusDays(heatMapConfiguration.historicalDays)
        val pastBlocksByDate = blockRepository.findAllBlocksCached(initiatorId, pastBlockStartDate, today)
            .map { it.atTimeZone(user.timeZone) }
            .groupBy { it.startDateTime.toLocalDate() }


        val futureBlockEndDate = today.plusDays(heatMapConfiguration.futureDays)
        val futureBlocksByDate = blockRepository.findAllBlocks(initiatorId, today, futureBlockEndDate)
            .map { it.atTimeZone(user.timeZone) }
            .flatMap { block -> block.splitByDate() }
            .groupBy { it.startDateTime.toLocalDate() }

        return HeatMapGenerator(today.toLocalDate(), futureBlockEndDate.toLocalDate(), weightContext)
            .generate(
                pastBlocksByDate,
                futureBlocksByDate,
                user.workingHours,
                user.timeZone,
                recipientTimezone
            )
    }
}