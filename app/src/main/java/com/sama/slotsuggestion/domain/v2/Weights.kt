package com.sama.slotsuggestion.domain.v2

import com.sama.meeting.application.MeetingSlotDTO
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.WorkingHours
import com.sama.slotsuggestion.domain.isNonWorkingDay
import com.sama.slotsuggestion.domain.v1.SlotSuggestion
import java.time.*
import java.util.function.Predicate.not
import kotlin.math.min
import kotlin.math.pow

interface Weigher {
    fun weight(heatMap: HeatMap): HeatMap
}

class PastBlockWeigher(private val inputBlock: Block) : Weigher {
    companion object {
        private const val weightWithRecipient = 10.0
        private const val weightWithoutRecipients = 5.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        val block = inputBlock.atTimeZone(heatMap.userTimeZone)
        if (block.multiDay() || block.allDay || block.zeroDuration()) {
            return heatMap
        }

        val weight = if (block.hasRecipients) {
            // there's a meeting with a person, we should add weight
            weightWithRecipient
        } else {
            // if it's a self-blocked time, we count as we DON'T want meetings for the time
            weightWithoutRecipients
        }

        return heatMap
            .query {
                inTimeRange(block.startDateTime.toLocalTime(), block.endDateTime.toLocalTime())
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
            .addFixedWeight(weight) { "past block: ${block.toDebugString()}" }
            .save(heatMap)
    }
}


class FutureBlockWeigher(private val block: Block) : Weigher {
    companion object {
        private const val weight = -10000.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        if (block.zeroDuration()) {
            return heatMap
        }

        return heatMap
            .query {
                from(block.startDateTime, heatMap.userTimeZone)
                to(block.endDateTime, heatMap.userTimeZone)
            }
            .addFixedWeight(weight) { "future block: ${block.toDebugString()}" }
            .save(heatMap)

    }
}

class WorkingHoursWeigher(private val workingHours: Map<DayOfWeek, WorkingHours>) : Weigher {
    companion object {
        private const val baseWeight = -900.0
        private const val maxWeight = -600.0
        private const val weightStep = 12.5
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        var result = heatMap

        for (dow in DayOfWeek.values()) {
            val wh = workingHours[dow]
            if (wh.isNonWorkingDay()) {
                result = result.query(slotQuery { dayOfWeek(dow) })
                    .addFixedWeight(baseWeight) { "non working day: $dow" }
                    .save(result)
            } else {
                check(wh != null)
                val query = slotQuery { dayOfWeek(dow) }
                    .and(not(slotQuery { inTimeRange(wh.startTime, wh.endTime) }))

                result = result.query(query)
                    .grouped()
                    .mapValue { groupInfo, slot ->
                        val isStartOfDay = groupInfo.chunkIdx % 2 == 1 // Every other slot group is the start of day
                        var fraction = (groupInfo.itemIdx.toDouble() / (groupInfo.size - 1)).pow(2.0)
                        if (isStartOfDay) fraction = 1 - fraction // reverse direction for start of day
                        val w = min(baseWeight + (fraction * weightStep * groupInfo.size), maxWeight)
                        slot.addWeight(w, "working hours: $dow - $wh")
                    }
                    .save(result)
            }
        }
        return result
    }
}

class RecipientTimeZoneWeigher(private val recipientTimeZone: ZoneId) : Weigher {
    companion object {
        private const val baseWeight = -1000.0
        private const val weightStep = 10.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        val now = LocalDateTime.now()
        val userOffsetSeconds = heatMap.userTimeZone.rules.getOffset(now).totalSeconds
        val requestOffsetSeconds = recipientTimeZone.rules.getOffset(now).totalSeconds
        val offsetDifference = userOffsetSeconds.toLong() - requestOffsetSeconds
        if (offsetDifference == 0L) {
            return heatMap
        }

        val startTime = LocalTime.of(9, 0).plusSeconds(offsetDifference)
        val endTime = LocalTime.of(17, 0).plusSeconds(offsetDifference)

        return heatMap.query(not(slotQuery { inTimeRange(startTime, endTime) }))
            .grouped()
            .addParabolicWeight(baseWeight, weightStep) { "recipient time zone preferred: $recipientTimeZone" }
            .save(heatMap)
    }
}

class SearchBoundaryWeigher : Weigher {
    companion object {
        private const val weight = -10000.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        val searchStartDateTime = ZonedDateTime.now(heatMap.userTimeZone)
        return heatMap
            .query { to(searchStartDateTime.plusMinutes(heatMap.intervalMinutes), heatMap.userTimeZone) }
            .addFixedWeight(weight) { "search boundary" }
            .save(heatMap)
    }
}

class RecencyWeigher : Weigher {
    override fun weight(heatMap: HeatMap): HeatMap {
        val startDate = heatMap.startDate.plusDays(3)
        return heatMap
            .query { fromDate(startDate) }
            .grouped()
            .addLinearWeight(startValue = 0.0, endValue = -20.0) { "recency " }
            .save(heatMap)
    }
}

class SuggestedSlotWeigher(private val ss: SlotSuggestion) : Weigher {
    companion object {
        private const val weight = -1000.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        return heatMap
            .query {
                from(ss.startDateTime.minusHours(4), heatMap.userTimeZone)
                to(ss.endDateTime.plusHours(4), heatMap.userTimeZone)
            }
            .addFixedWeight(weight) { "suggested slot: ${ss.startDateTime} - ${ss.endDateTime}" }
            .save(heatMap)
    }
}

class TimeVarietyWeigher(private val ss: Collection<SlotSuggestion>) : Weigher {
    companion object {
        private const val baseWeight = -50.0
    }

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
                .query { inTimeRange(repeatingSlot.first, repeatingSlot.second) }
                .addFixedWeight(weight) { "suggested time variety repeating $count times: ${repeatingSlot.first} - ${repeatingSlot.second}" }
                .save(result)
        }

        return result
    }

}

class FutureProposedSlotWeigher(private val ms: MeetingSlotDTO) : Weigher {
    companion object {
        private const val weight = 5.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        return heatMap
            .query {
                from(ms.startDateTime, heatMap.userTimeZone)
                to(ms.endDateTime, heatMap.userTimeZone)
            }
            .addFixedWeight(weight) { "proposed slot: ${ms.startDateTime} - ${ms.endDateTime}" }
            .save(heatMap)
    }

}