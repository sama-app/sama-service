package com.sama.slotsuggestion.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.sentry.sentrySpan
import com.sama.meeting.application.MeetingDataService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.HeatMap
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.weigher
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.stereotype.Service

@ApplicationService
@Service
class HeatMapService(
    private val userRepository: UserRepository,
    private val googleCalendarService: GoogleCalendarService,
    private val meetingDataService: MeetingDataService,
    private val heatMapConfiguration: HeatMapConfiguration,
    private val clock: Clock,
) {
    fun generate(initiatorId: UserId, recipientTimezone: ZoneId): HeatMap {
        val user = userRepository.findById(initiatorId)

        val today = LocalDate.now(clock.withZone(user.timeZone)).atStartOfDay(user.timeZone)
        val pastBlockStartDate = today.minusDays(heatMapConfiguration.historicalDays)
        val futureBlockEndDate = today.plusDays(heatMapConfiguration.futureDays)

        val (pastBlocks, futureBlocks) = googleCalendarService.findEvents(
            userId = initiatorId,
            startDateTime = pastBlockStartDate,
            endDateTime = futureBlockEndDate
        ).map { it.toBlock(user.timeZone) }
            .partition { it.startDateTime < today }

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

    private fun CalendarEvent.toBlock(timeZone: ZoneId): Block {
        return Block(startDateTime, endDateTime, eventData.allDay,
            eventData.attendeeCount > 0,
            aggregatedData!!.recurrenceCount
        ).atTimeZone(timeZone)
    }
}