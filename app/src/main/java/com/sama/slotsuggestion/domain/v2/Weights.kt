package com.sama.slotsuggestion.domain.v2

import com.sama.meeting.application.MeetingSlotDTO
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.WorkingHours
import com.sama.slotsuggestion.domain.v1.SlotSuggestion
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
    private val weightWithoutRecipients = 5.0

    override fun weight(heatMap: HeatMap): HeatMap {
        val block = inputBlock.atTimeZone(heatMap.userTimeZone)
        if (block.multiDay() || block.allDay || block.zeroDuration()) {
            return heatMap
        }

        return heatMap
            .query {
                fromTime = block.startDateTime.toLocalTime()
                toTime = block.endDateTime.toLocalTime()
                // recurring blocks affect days of weeks
                if (block.isRecurring()) {
                    dayOfWeek(block.startDateTime.dayOfWeek)
                }
                // regular meetings affect either workdays or weekends
                else if (block.startDateTime.dayOfWeek.isWorkday()) {
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
                }

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
            .query { to(searchStartDateTime.plusMinutes(heatMap.intervalMinutes), heatMap.userTimeZone) }
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

class TimeVarietyWeigher(private val ss: Collection<SlotSuggestion>) : Weigher {
    private val baseWeight = -30.0

    override fun weight(heatMap: HeatMap): HeatMap {
        val repeatingSlots = ss
            .groupBy { it.startDateTime.toLocalTime() to it.endDateTime.toLocalTime() }
            .mapValues { it.value.size }

        if (repeatingSlots.isEmpty()) {
            return heatMap
        }

        var result = heatMap
        for ((repeatingSlot, count) in repeatingSlots) {
            val weight = baseWeight * count

            result = result
                .query {
                    fromTime = repeatingSlot.first
                    toTime = repeatingSlot.second
                }
                .modify { _, slot ->
                    slot.addWeight(
                        "suggested time variety repeating $count times: ${repeatingSlot.first} - ${repeatingSlot.second}",
                        weight
                    )
                }
                .save(result)
        }

        return result
    }

}

class FutureProposedSlotWeigher(private val ms: MeetingSlotDTO) : Weigher {
    private val weight = 5.0

    override fun weight(heatMap: HeatMap): HeatMap {
        return heatMap
            .query {
                from(ms.startDateTime, heatMap.userTimeZone)
                to(ms.endDateTime, heatMap.userTimeZone)
            }
            .modify { _, slot -> slot.addWeight("proposed slot: ${ms.startDateTime} - ${ms.endDateTime}", weight) }
            .save(heatMap)
    }

}