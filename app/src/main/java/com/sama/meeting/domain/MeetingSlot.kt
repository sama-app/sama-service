package com.sama.meeting.domain

import com.sama.calendar.domain.Block
import com.sama.common.ValueObject
import java.time.Duration
import java.time.Period
import java.time.ZonedDateTime
import javax.persistence.Embeddable

@Embeddable
@ValueObject
data class MeetingSlot(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
) {

    fun isRange(meetingDuration: Duration): Boolean {
        return duration() != meetingDuration
    }

    fun duration(): Duration {
        return Duration.between(startTime, endTime)
    }

    fun expandBy(duration: Duration, interval: Duration): List<ZonedDateTime> {
        val overtime = duration().minus(duration)
        if (overtime.isNegative) {
            return emptyList()
        }

        val slotCount = overtime.dividedBy(interval)
        if (slotCount == 0L) {
            return listOf(startTime)
        }

        val intervalMinutes = interval.toMinutes()
        return 0L.until(slotCount)
            .map { startTime.plusMinutes(intervalMinutes * it) }
    }

    fun overlaps(block: Block): Boolean {
        return startTime.isBefore(block.endDateTime) && block.startDateTime.isEqual(endTime)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is MeetingSlot) {
            return false
        }
        return this.startTime.isEqual(other.startTime)
                && this.endTime.isEqual(other.endTime)
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        return result
    }
}
