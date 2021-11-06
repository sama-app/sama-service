package com.sama.meeting.domain

import com.sama.common.ValueObject
import com.sama.common.ZonedDateTimeRange
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.Embeddable

val MEETING_SLOT_INTERVAL: Duration = Duration.ofMinutes(15)

@Embeddable
@ValueObject
data class MeetingSlot(
    override val startDateTime: ZonedDateTime,
    override val endDateTime: ZonedDateTime
) : ZonedDateTimeRange {

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

    fun atTimeZone(zoneId: ZoneId): MeetingSlot {
        return MeetingSlot(startDateTime.withZoneSameInstant(zoneId), endDateTime.withZoneSameInstant(zoneId))
    }

    fun overlaps(slot: ZonedDateTimeRange): Boolean {
        return startDateTime.isBefore(slot.endDateTime) && endDateTime.isAfter(slot.startDateTime)
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
