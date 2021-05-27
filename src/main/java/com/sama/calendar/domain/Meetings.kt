package com.sama.calendar.domain

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.aventrix.jnanoid.jnanoid.NanoIdUtils.randomNanoId
import com.sama.common.*
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
    CONFIRMED
}

@DomainService
data class MeetingCodeGenerator(val codeLength: Int) {
    @Factory
    companion object {
        private const val defaultCodeLength = 10
        fun default(): MeetingCodeGenerator {
            return MeetingCodeGenerator(defaultCodeLength)
        }
    }

    fun generate(): String {
        return randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, codeLength)
    }
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
                meetingEntity.id!!, meetingEntity.initiatorId!!, ofMinutes(meetingEntity.durationMinutes!!),
                meetingEntity.slots
                    .filter { it.status == MeetingSlotStatus.SUGGESTED }
                    .map { MeetingSlot(it.startDateTime, it.endDateTime) },
                meetingEntity.recipientEmail?.let { MeetingRecipient.fromEmail(it) }
            ))
        }
    }

    init {
        if (duration !in allowedDurations) {
            throw InvalidDurationException(meetingId, duration)
        }
        validateSlots(suggestedSlots)
    }

    fun suggestSlots(slots: List<MeetingSlot>): Result<InitiatedMeeting> {
        kotlin.runCatching { validateSlots(slots) }
            .onFailure { return failure(it) }

        return success(copy(suggestedSlots = suggestedSlots.plus(slots)))
    }

    fun propose(proposedSlots: List<MeetingSlot>, meetingCode: MeetingCode): Result<ProposedMeeting> {
        if (proposedSlots.isEmpty()) {
            return failure(InvalidMeetingProposalException(meetingId, "No slots proposed"))
        }

        kotlin.runCatching { validateSlots(proposedSlots) }
            .onFailure { return failure(it) }

        return success(
            ProposedMeeting(meetingId, initiatorId, duration, proposedSlots, meetingRecipient, meetingCode)
        )
    }

    private fun validateSlots(slots: List<MeetingSlot>) {
        // TODO: validate duplicates

        slots.firstOrNull { it.duration() < duration }
            ?.run {
                throw InvalidMeetingSlotException(meetingId, this)
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
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            return success(
                ProposedMeeting(
                    meetingEntity.id!!, meetingEntity.initiatorId!!, ofMinutes(meetingEntity.durationMinutes!!),
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

    fun confirm(slot: MeetingSlot, recipient: MeetingRecipient): Result<ConfirmedMeeting> {
        val confirmedSlot = proposedSlots.find { it == slot }
            ?: return failure(NotFoundException(MeetingSlotEntity::class, slot))

        // TODO: validate recipients matching

        return success(
            ConfirmedMeeting(meetingId, initiatorId, duration, recipient, confirmedSlot)
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