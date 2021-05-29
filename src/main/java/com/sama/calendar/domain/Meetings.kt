package com.sama.calendar.domain

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.aventrix.jnanoid.jnanoid.NanoIdUtils.randomNanoId
import com.sama.common.*
import com.sama.users.domain.UserId
import liquibase.pro.packaged.it
import java.time.Duration
import java.time.Duration.ofMinutes
import java.time.ZonedDateTime
import javax.persistence.Embeddable
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

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
data class MeetingIntent(
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val recipientId: UserId?,
    val duration: Duration,
    val suggestedSlots: List<MeetingSlot>,
) {
    val allowedDurations = setOf(ofMinutes(30), ofMinutes(45), ofMinutes(60))

    @Factory
    companion object {
        fun of(meetingIntentEntity: MeetingIntentEntity): Result<MeetingIntent> {
            return kotlin.runCatching {
                MeetingIntent(
                    meetingIntentEntity.id!!, meetingIntentEntity.initiatorId!!,
                    meetingIntentEntity.recipientId,
                    ofMinutes(meetingIntentEntity.durationMinutes!!),
                    meetingIntentEntity.suggestedSlots
                        .map { MeetingSlot(it.startDateTime, it.endDateTime) }
                )
            }
        }
    }

    init {
        if (duration !in allowedDurations) {
            throw InvalidDurationException(meetingIntentId, duration)
        }
        validateSlots(suggestedSlots)
    }

    fun propose(
        meetingProposalId: MeetingProposalId,
        meetingCode: MeetingCode,
        proposedSlots: List<MeetingSlot>
    ): Result<MeetingProposal> {
        if (proposedSlots.isEmpty()) {
            return failure(InvalidMeetingProposalException(meetingIntentId, "No slots proposed"))
        }

        kotlin.runCatching { validateSlots(proposedSlots) }
            .onFailure { return failure(it) }

        return success(
            MeetingProposal(meetingProposalId, meetingIntentId, initiatorId, duration, proposedSlots, meetingCode)
        )
    }

    private fun validateSlots(slots: List<MeetingSlot>) {
        // TODO: validate duplicates

        slots.firstOrNull { it.duration() < duration }
            ?.run {
                throw InvalidMeetingSlotException(meetingIntentId, this)
            }
    }
}

enum class MeetingProposalStatus {
    INITIATED,
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED,
}

@DomainEntity
data class MeetingProposal(
    val meetingProposalId: MeetingProposalId,
    val meetingIntentId: MeetingIntentId,
    val initiatorId: UserId,
    val duration: Duration,
    val proposedSlots: List<MeetingSlot>,
    val meetingCode: MeetingCode
) {
    val status = MeetingProposalStatus.PROPOSED
    val slotInterval = ofMinutes(15)

    @Factory
    companion object {
        fun of(
            meetingIntentEntity: MeetingIntentEntity,
            meetingProposalEntity: MeetingProposalEntity
        ): Result<MeetingProposal> {
            if (meetingProposalEntity.status != MeetingProposalStatus.PROPOSED) {
                return failure(NotFoundException(MeetingProposal::class, meetingProposalEntity.id))
            }

            val proposedSlots = meetingProposalEntity.proposedSlots
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            return success(
                MeetingProposal(
                    meetingProposalEntity.id!!,
                    meetingIntentEntity.id!!,
                    meetingIntentEntity.initiatorId!!,
                    ofMinutes(meetingIntentEntity.durationMinutes!!),
                    proposedSlots,
                    meetingProposalEntity.code!!
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
            ?: return failure(NotFoundException(MeetingSuggestedSlotEntity::class, slot))


        return success(
            ConfirmedMeeting(meetingProposalId, initiatorId, duration, recipient, confirmedSlot)
        )
    }
}

@DomainEntity
data class ConfirmedMeeting(
    val meetingProposalId: MeetingProposalId,
    val initiatorId: UserId,
    val duration: Duration,
    val meetingRecipient: MeetingRecipient,
    val slot: MeetingSlot
) {
    val status = MeetingProposalStatus.CONFIRMED
}

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

@Embeddable
@ValueObject
data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromEmail(email: String): MeetingRecipient {
            return MeetingRecipient(null, email)
        }
    }
}