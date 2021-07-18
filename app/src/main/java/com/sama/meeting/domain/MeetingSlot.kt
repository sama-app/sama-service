package com.sama.meeting.domain

import com.sama.calendar.domain.Event
import com.sama.common.ValueObject
import java.time.Duration
import java.time.ZonedDateTime
import javax.persistence.Embeddable

@Embeddable
@ValueObject
data class MeetingSlot(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime
) {

    fun isRange(meetingDuration: Duration): Boolean {
        return duration() != meetingDuration
    }

    fun duration(): Duration {
        return Duration.between(startDateTime, endDateTime)
    }

    fun expandBy(duration: Duration, interval: Duration): List<ZonedDateTime> {
        val overtime = duration().minus(duration)
        if (overtime.isNegative) {
            return emptyList()
        }

        val slotCount = overtime.dividedBy(interval) + 1
        if (slotCount == 0L) {
            return listOf(startDateTime)
        }

        val intervalMinutes = interval.toMinutes()
        return 0L.until(slotCount)
            .map { startDateTime.plusMinutes(intervalMinutes * it) }
    }

    fun overlaps(event: Event): Boolean {
        return startDateTime.isBefore(event.endDateTime) && endDateTime.isAfter(event.startDateTime)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is MeetingSlot) {
            return false
        }
        return this.startDateTime.isEqual(other.startDateTime)
                && this.endDateTime.isEqual(other.endDateTime)
    }

    override fun hashCode(): Int {
        var result = startDateTime.hashCode()
        result = 31 * result + endDateTime.hashCode()
        return result
    }
}
