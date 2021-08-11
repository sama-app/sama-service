package com.sama.slotsuggestion.domain.v2

import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.v1.SlotSuggestion
import com.sama.slotsuggestion.domain.WorkingHours
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

interface Weigher {
    fun weight(heatMap: HeatMap): HeatMap
}

class PastBlockWeigher(private val inputBlock: Block) : Weigher {
    private val weightWithRecipient = 10.0
    private val weightWithoutRecipients = -10.0

    override fun weight(heatMap: HeatMap): HeatMap {
        val block = inputBlock.atTimeZone(heatMap.userTimeZone)
        if (block.multiDay() || block.allDay || block.zeroDuration()) {
            return heatMap
        }

        return heatMap
            .query {
                fromTime = block.startDateTime.toLocalTime()
                toTime = block.endDateTime.toLocalTime()
                if (block.startDateTime.dayOfWeek.isWorkday()) {
                    workdays()
                } else {
                    weekend()
                }
            }
            .modify { _, slot ->
                val weight = if (block.hasRecipients) {
                    // there's a meeting with a person, we should add weight
                    weightWithRecipient
                } else {
                    // if it's a self-blocked time, we count as we DON'T want meetings for the time
                    weightWithoutRecipients
                } / block.recurrenceCount // count recurring events only once

                slot.addWeight("past block: ${block.toDebugString()}", weight)
            }
            .save(heatMap)
    }
}


class FutureBlockWeigher(private val block: Block) : Weigher {
    private val weight = -10000.0

    override fun weight(heatMap: HeatMap): HeatMap {
        if (block.zeroDuration()) {
            return heatMap
        }

        return heatMap
            .query {
                from(block.startDateTime, heatMap.userTimeZone)
                to(block.endDateTime, heatMap.userTimeZone)
            }
            .modify { _, slot -> slot.addWeight("future block: ${block.toDebugString()}", weight) }
            .save(heatMap)

    }
}

class WorkingHoursWeigher(private val workingHours: Map<DayOfWeek, WorkingHours>) : Weigher {
    private val weight = -1000.0

    override fun weight(heatMap: HeatMap): HeatMap {
        var result = heatMap

        for (dow in DayOfWeek.values()) {
            val wh = workingHours[dow]
            val startTime = wh?.startTime ?: LocalTime.MIN
            val endTime = wh?.endTime ?: LocalTime.MIN

            val slotQuery = slotQuery {
                dayOfWeek(dow)
                this.toTime = startTime
            }.or(slotQuery {
                dayOfWeek(dow)
                this.fromTime = endTime
            })

            result = result
                .query(slotQuery)
                .modify { _, slot -> slot.addWeight("working hours: $dow - $wh", weight) }
                .save(result)
        }
        return result
    }
}

class RecipientTimeZoneWeigher(private val recipientTimeZone: ZoneId) : Weigher {
    private val weight = -50.0

    override fun weight(heatMap: HeatMap): HeatMap {
        val now = LocalDateTime.now()
        val userOffsetSeconds = heatMap.userTimeZone.rules.getOffset(now).totalSeconds
        val requestOffsetSeconds = recipientTimeZone.rules.getOffset(now).totalSeconds
        val offsetDifference = userOffsetSeconds.toLong() - requestOffsetSeconds
        if (offsetDifference == 0L) {
            return heatMap
        }

        val startTime = LocalTime.of(8, 0).plusSeconds(offsetDifference)
        val endTime = LocalTime.of(20, 0).plusSeconds(offsetDifference)

        val slotQuery = slotQuery {
            this.toTime = startTime
        }.or(slotQuery {
            this.fromTime = endTime
        })

        return heatMap
            .query(slotQuery)
            .modify { _, slot -> slot.addWeight("recipient time zone preferred: $recipientTimeZone", weight) }
            .save(heatMap)
    }
}

class SearchBoundaryWeigher : Weigher {
    private val weight = -10000.0

    override fun weight(heatMap: HeatMap): HeatMap {
        val searchStartDateTime = ZonedDateTime.now(heatMap.userTimeZone)
        return heatMap
            .query { to(searchStartDateTime, heatMap.userTimeZone) }
            .modify { _, slot -> slot.addWeight("search boundary", weight) }
            .save(heatMap)
    }
}

class RecencyWeigher : Weigher {
    private val weightIncrement = -0.01

    override fun weight(heatMap: HeatMap): HeatMap {
        val startDate = heatMap.startDate.plusDays(4)
        return heatMap
            .query { this.fromDate = startDate }
            .modify { idx, slot -> slot.addWeight("recency", weightIncrement * idx) }
            .save(heatMap)
    }
}

class SuggestedSlotWeigher(private val ss: SlotSuggestion) : Weigher {
    private val weight = -1000.0

    override fun weight(heatMap: HeatMap): HeatMap {
        return heatMap
            .query {
                from(ss.startDateTime.minusHours(4), heatMap.userTimeZone)
                to(ss.endDateTime.plusHours(4), heatMap.userTimeZone)
            }
            .modify { _, slot -> slot.addWeight("suggested slot: ${ss.startDateTime} - ${ss.endDateTime}", weight) }
            .save(heatMap)
    }
}