package com.sama.slotsuggestion.domain

import com.sama.meeting.application.MeetingSlotDTO
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField.DAY_OF_WEEK
import java.time.temporal.ChronoUnit.DAYS
import java.util.function.Predicate
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


class FutureBlocksWeigher(private val blocks: Collection<Block>) : Weigher {
    companion object {
        private const val weight = -10000.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        if (blocks.isEmpty()) {
            return heatMap
        }

        val query = blocks.asSequence()
            .filter { !it.zeroDuration() }
            .map {
                slotQuery {
                    from(it.startDateTime, heatMap.userTimeZone)
                    to(it.endDateTime, heatMap.userTimeZone)
                } as Predicate<Slot>
            }
            .reduce { acc, slotQuery -> acc.or(slotQuery) }

        return heatMap.query(query)
            .addFixedWeight(weight) { "future block" }
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
    companion object {
        private const val hoursToBlockFromNow = 4L
        private const val currentTimeValue = -1000.0

        private const val startValue = 0.0
        private const val endValue = -20.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        var result = heatMap
        // block out time from now + X hours
        val currentDateTimeBlockEnd = LocalDateTime.now(heatMap.userTimeZone).plusHours(hoursToBlockFromNow)
        result = result.query { to(currentDateTimeBlockEnd) }
            .addFixedWeight(currentTimeValue) { "Suggestion too close to now" }
            .save(result)

        // gradually add weights for the future
        val startDate = heatMap.startDate.plusDays(3)
        result = result
            .query { fromDate(startDate) }
            .grouped()
            .addLinearWeight(startValue = startValue, endValue = endValue) { "Prefer more recent times " }
            .save(result)
        return result
    }
}

class SuggestedSlotWeigher(private val ss: Collection<SlotSuggestion>) : Weigher {
    companion object {
        private const val weight = -1000.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        if (ss.isEmpty()) {
            return heatMap
        }
        val query = ss.asSequence()
            .map {
                slotQuery {
                    from(it.startDateTime.minusHours(4), heatMap.userTimeZone)
                    to(it.endDateTime.plusHours(3), heatMap.userTimeZone)
                } as Predicate<Slot>
            }
            .reduce { acc, slotQuery -> acc.or(slotQuery) }

        return heatMap
            .query(query)
            .addFixedWeight(weight) { "suggested slot" }
            .save(heatMap)
    }
}

class ThisOrNextWeekTemplateWeigher(private val ss: Collection<SlotSuggestion>) : Weigher {
    companion object {
        private const val repeatingTimeBaseWeight = -50.0
        private const val repeatingWeekWeight = -500.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        if (ss.isEmpty()) {
            return heatMap
        }

        // Vary suggested times (decrease weight of repeating slot times)
        val repeatingSlotsByTime = ss
            .groupBy { it.startDateTime.toLocalTime() to it.endDateTime.toLocalTime() }
            .mapValues { it.value.size }

        var result = heatMap
        for ((repeatingSlot, count) in repeatingSlotsByTime) {
            val weight = repeatingTimeBaseWeight * count

            result = result
                .query { inTimeRange(repeatingSlot.first, repeatingSlot.second) }
                .addFixedWeight(weight) { "Suggested time variety (repeating $count times)" }
                .save(result)
        }

        // Vary suggested weeks (no more than two slots per week)
        val startOfThisWeekDate = heatMap.startDate
            .with(DAY_OF_WEEK, 1)

        val repeatingSlotsByWeek = ss
            .groupBy { startOfThisWeekDate.until(it.startDateTime.toLocalDate(), DAYS) / 7 }
            .mapValues { it.value.size }

        for ((weekNumber, count) in repeatingSlotsByWeek) {
            if (count == 1) {
                continue
            }

            result = result
                .query {
                    fromDate(startOfThisWeekDate.plusWeeks(weekNumber))
                    toDate(startOfThisWeekDate.plusWeeks(weekNumber + 1).minusDays(1))
                }
                .addFixedWeight(repeatingWeekWeight) { "Suggest week variety (repeating $count times)" }
                .save(result)
        }

        return result
    }
}

class FutureProposedSlotWeigher(private val slots: List<MeetingSlotDTO>) : Weigher {
    companion object {
        private const val baseWeight = 15.0
        private const val maxWeight = 75.0
    }

    override fun weight(heatMap: HeatMap): HeatMap {
        val proposedSlotRepeatingCounts = slots
            .groupingBy { it }
            .eachCount()

        var result = heatMap
        for ((ms, repeatingCount) in proposedSlotRepeatingCounts) {
            val weight = min(baseWeight * repeatingCount, maxWeight)
            result = result
                .query {
                    from(ms.startDateTime, heatMap.userTimeZone)
                    to(ms.endDateTime, heatMap.userTimeZone)
                }
                .addFixedWeight(weight) { "proposed slot: ${ms.startDateTime} - ${ms.endDateTime} (repeating $repeatingCount times)" }
                .save(result)
        }

        return result
    }
}