package com.sama.calendar.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory
import com.sama.common.NotFoundException
import com.sama.common.replace
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.Duration.ofMinutes
import java.time.ZonedDateTime
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

enum class MeetingStatus {
    INITIATED,
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

enum class MeetingSlotStatus {
    SUGGESTED,
    PROPOSED,
    REMOVED,
}

@DomainEntity
data class InitiatedMeeting(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val suggestedSlots: List<MeetingSlot>,
    val meetingRecipient: MeetingRecipient?
) {
    val status = MeetingStatus.INITIATED
    val allowedDurations = setOf(ofMinutes(30), ofMinutes(45), ofMinutes(60))

    @Factory
    companion object {
        fun of(meetingEntity: MeetingEntity): Result<InitiatedMeeting> {
            if (meetingEntity.status != MeetingStatus.INITIATED) {
                return failure(NotFoundException(InitiatedMeeting::class, meetingEntity.id))
            }

            return success(InitiatedMeeting(
                meetingEntity.id!!, meetingEntity.initiatorId!!, meetingEntity.duration!!,
                meetingEntity.slots.map { MeetingSlot(it.id!!, it.status, it.startDateTime, it.endDateTime) },
                meetingEntity.recipientEmail?.let { MeetingRecipient.fromEmail(it) }
            ))
        }
    }

    init {
        if (duration !in allowedDurations) {
            throw UnsupportedDurationException(meetingId, duration)
        }
        validateSlots(suggestedSlots)
    }

    fun suggestSlots(slots: List<MeetingSlot>): Result<InitiatedMeeting> {
        kotlin.runCatching { validateSlots(slots) }
            .onFailure { return failure(it) }

        return success(copy(suggestedSlots = suggestedSlots.plus(slots)))
    }

    fun removeSlot(slotId: SlotId): Result<InitiatedMeeting> {
        val slot = suggestedSlots.find { it.meetingSlotId == slotId }
            ?: return failure(NotFoundException(MeetingSlotEntity::class, slotId))

        val removedSlot = slot.copy(status = MeetingSlotStatus.REMOVED)

        return success(copy(suggestedSlots = suggestedSlots.replace(removedSlot) { it == slot }))
    }

    fun propose(proposedSlotIds: Set<SlotId>, meetingCode: MeetingCode): Result<ProposedMeeting> {
        val proposedSlots = suggestedSlots
            .filter { it.meetingSlotId in proposedSlotIds }
            .map { it.copy(status = MeetingSlotStatus.PROPOSED) }

        if (proposedSlots.size != proposedSlotIds.size) {
            return failure(NotFoundException(MeetingSlotEntity::class, proposedSlotIds))
        }

        return success(
            ProposedMeeting(meetingId, initiatorId, duration, proposedSlots, meetingRecipient, meetingCode)
        )
    }

    private fun validateSlots(slots: List<MeetingSlot>) {
        // TODO: validate duplicates

        slots.firstOrNull { it.duration() < duration }
            ?.run {
                throw InvalidSuggestedSlotException(meetingId, this)
            }
    }
}

@DomainEntity
data class ProposedMeeting(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val proposedSlots: List<MeetingSlot>,
    val meetingRecipient: MeetingRecipient?,
    val meetingCode: MeetingCode
) {
    val status = MeetingStatus.PROPOSED
    val slotInterval = ofMinutes(15)

    @Factory
    companion object {
        fun of(meetingEntity: MeetingEntity): Result<ProposedMeeting> {
            if (meetingEntity.status != MeetingStatus.PROPOSED) {
                return failure(NotFoundException(ProposedMeeting::class, meetingEntity.id))
            }

            val proposedSlots = meetingEntity.slots
                .filter { it.status == MeetingSlotStatus.PROPOSED }
                .map { MeetingSlot(it.id!!, it.status, it.startDateTime, it.endDateTime) }
            return success(
                ProposedMeeting(
                    meetingEntity.id!!, meetingEntity.initiatorId!!, meetingEntity.duration!!,
                    proposedSlots, meetingEntity.recipientEmail?.let { MeetingRecipient.fromEmail(it) },
                    meetingEntity.code!!
                )
            )
        }
    }

    fun expandedSlots(): List<MeetingSlot> {
        return proposedSlots.flatMap { slot ->
            if (!slot.isRange(duration)) {
                slot.expandBy(duration, slotInterval)
                    .map { slot.copy(startTime = it, endTime = it.plus(duration)) }
            } else {
                listOf(slot)
            }
        }
    }

    fun confirm(slotId: SlotId, recipient: MeetingRecipient): Result<ConfirmedMeeting> {
        val slot = proposedSlots.find { it.meetingSlotId == slotId }
            ?: return failure(NotFoundException(MeetingSlotEntity::class, slotId))

        // TODO: validate recipients matching

        return success(
            ConfirmedMeeting(meetingId, initiatorId, duration, recipient, slot)
        )
    }
}

@DomainEntity
data class ConfirmedMeeting(
    val meetingId: MeetingId,
    val initiatorId: UserId,
    val duration: Duration,
    val meetingRecipient: MeetingRecipient,
    val slot: MeetingSlot
) {
    val status = MeetingStatus.CONFIRMED
}

@DomainEntity
data class MeetingSlot(
    val meetingSlotId: SlotId,
    val status: MeetingSlotStatus,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
) {
    @Factory
    companion object {
        fun new(meetingSlotId: SlotId, startTime: ZonedDateTime, endTime: ZonedDateTime): MeetingSlot {
            return MeetingSlot(meetingSlotId, MeetingSlotStatus.SUGGESTED, startTime, endTime)
        }
    }

    fun isRange(duration: Duration): Boolean {
        return duration() != duration
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
}

@DomainEntity
data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromEmail(email: String): MeetingRecipient {
            return MeetingRecipient(null, email)
        }
    }
}