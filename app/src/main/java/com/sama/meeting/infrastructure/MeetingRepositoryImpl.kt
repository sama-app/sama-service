package com.sama.meeting.infrastructure

import com.sama.common.Factory
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.MeetingRecipient
import com.sama.meeting.domain.Meeting
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.RejectedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.meeting.infrastructure.jpa.MeetingEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.meeting.infrastructure.jpa.MeetingJpaRepository
import com.sama.meeting.infrastructure.jpa.MeetingRecipientEntity
import com.sama.meeting.infrastructure.jpa.MeetingSlotStatus
import com.sama.meeting.infrastructure.jpa.findByCodeOrThrow
import com.sama.meeting.infrastructure.jpa.findLockedByCodeOrThrow
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.time.Duration
import java.time.ZonedDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class MeetingRepositoryImpl(
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingIntentJpaRepository: MeetingIntentJpaRepository,
) : MeetingRepository {

    override fun nextIdentity(): MeetingId {
        return meetingJpaRepository.nextIdentity().toMeetingId()
    }

    override fun findByIdOrThrow(meetingId: MeetingId): Meeting {
        val meetingEntity = meetingJpaRepository.findByIdOrThrow(meetingId.id)
        val intentEntity = meetingIntentJpaRepository.findByIdOrThrow(meetingEntity.meetingIntentId!!)
        return meetingFrom(intentEntity, meetingEntity)
    }

    override fun findByCodeOrThrow(code: MeetingCode, forUpdate: Boolean): Meeting {
        val meetingEntity = if (forUpdate) meetingJpaRepository.findLockedByCodeOrThrow(code.code)
        else meetingJpaRepository.findByCodeOrThrow(code.code)

        val intentEntity = meetingIntentJpaRepository.findByIdOrThrow(meetingEntity.meetingIntentId!!)
        return meetingFrom(intentEntity, meetingEntity)
    }

    override fun findAllExpiring(expiryDateTime: ZonedDateTime): Collection<ExpiredMeeting> {
        return meetingJpaRepository.findAllIdsExpiring(expiryDateTime)
            .map { ExpiredMeeting(it.toMeetingId()) }
    }

    override fun save(meeting: Meeting): Meeting {
        val meetingEntity: MeetingEntity
        when (meeting) {
            is ConfirmedMeeting -> {
                meetingEntity = meetingJpaRepository.findByIdOrThrow(meeting.meetingId.id)
                meetingEntity.applyChanges(meeting)
            }
            is ProposedMeeting -> {
                val entity = meetingJpaRepository.findByIdOrNull(meeting.meetingId.id)
                if (entity == null) {
                    meetingEntity = MeetingEntity.new(meeting)
                } else {
                    meetingEntity = entity.applyChanges(meeting)
                }
            }
            is ExpiredMeeting -> throw UnsupportedOperationException()
            is RejectedMeeting -> throw UnsupportedOperationException()
        }
        meetingJpaRepository.save(meetingEntity)
        return meeting
    }

    override fun saveAllExpired(meetings: Collection<ExpiredMeeting>) {
        meetings.map { it.meetingId.id }
            .chunked(1000)
            .forEach {
                meetingJpaRepository.updateStatus(MeetingStatus.EXPIRED, it)
            }
    }

    override fun findAllProposedSlots(
        initiatorId: UserId,
        fromDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
    ): Collection<MeetingSlot> {
        return meetingJpaRepository.findAllProposedSlots(initiatorId, fromDateTime, endDateTime)
    }

    @Factory
    private fun meetingFrom(
        meetingIntentEntity: MeetingIntentEntity,
        meetingEntity: MeetingEntity,
    ): Meeting {
        return when (meetingEntity.status!!) {
            MeetingStatus.PROPOSED -> {
                val (proposedSlots, rejectedSlots) = meetingEntity.proposedSlots
                    .partition { it.status == MeetingSlotStatus.PROPOSED }

                ProposedMeeting(
                    meetingEntity.id!!.toMeetingId(),
                    meetingIntentEntity.id!!.toMeetingIntentId(),
                    Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                    meetingIntentEntity.initiatorId!!.toUserId(),
                    meetingEntity.meetingRecipient?.recipientId?.let { UserId(it) },
                    meetingEntity.currentActor!!,
                    proposedSlots.map { MeetingSlot(it.startDateTime, it.endDateTime) },
                    rejectedSlots.map { MeetingSlot(it.startDateTime, it.endDateTime) },
                    meetingEntity.code!!.toMeetingCode(),
                    meetingEntity.title!!
                )
            }
            MeetingStatus.CONFIRMED -> ConfirmedMeeting(
                meetingEntity.id!!.toMeetingId(),
                meetingIntentEntity.initiatorId!!.toUserId(),
                Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                meetingEntity.meetingRecipient!!.toDomainObject(),
                meetingEntity.confirmedSlot!!,
                meetingEntity.title!!
            )
            MeetingStatus.REJECTED -> RejectedMeeting(meetingEntity.id!!.toMeetingId())
            MeetingStatus.EXPIRED -> ExpiredMeeting(meetingEntity.id!!.toMeetingId())
        }
    }
}

fun Long.toMeetingId(): MeetingId {
    return MeetingId(this)
}

fun String.toMeetingCode(): MeetingCode {
    return MeetingCode(this)
}

fun MeetingRecipientEntity.toDomainObject(): MeetingRecipient {
    if (recipientId != null) {
        return UserRecipient.of(recipientId.toUserId(), email!!)
    } else {
        return EmailRecipient.of(email!!)
    }
}

