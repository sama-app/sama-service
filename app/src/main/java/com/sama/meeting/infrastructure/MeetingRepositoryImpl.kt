package com.sama.meeting.infrastructure

import com.sama.common.Factory
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.meeting.infrastructure.jpa.MeetingJpaRepository
import com.sama.meeting.infrastructure.jpa.findByCodeOrThrow
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime

@Component
class MeetingRepositoryImpl(
    private val meetingJpaRepository: MeetingJpaRepository,
    private val meetingIntentJpaRepository: MeetingIntentJpaRepository
) : MeetingRepository {
    override fun nextIdentity(): MeetingId {
        return meetingJpaRepository.nextIdentity()
    }

    override fun findByIdOrThrow(id: MeetingId): Meeting {
        val meetingEntity = meetingJpaRepository.findByIdOrThrow(id)
        val intentEntity = meetingIntentJpaRepository.findByIdOrThrow(meetingEntity.meetingIntentId)
        return meetingFrom(intentEntity, meetingEntity)
    }

    override fun findByCodeOrThrow(code: MeetingCode): Meeting {
        val meetingEntity = meetingJpaRepository.findByCodeOrThrow(code)
        val intentEntity = meetingIntentJpaRepository.findByIdOrThrow(meetingEntity.meetingIntentId)

        return meetingFrom(intentEntity, meetingEntity)
    }

    override fun findAllExpiring(expiryDateTime: ZonedDateTime): Collection<ExpiredMeeting> {
        return meetingJpaRepository.findAllIdsExpiring(expiryDateTime)
            .map { ExpiredMeeting(it) }
    }

    override fun save(meeting: Meeting): Meeting {
        val meetingEntity: MeetingEntity
        when (meeting) {
            is ConfirmedMeeting -> {
                meetingEntity = meetingJpaRepository.findByIdOrThrow(meeting.meetingId)
                meetingEntity.applyChanges(meeting)
            }
            is ProposedMeeting -> {
                meetingEntity = MeetingEntity.new(meeting)
            }
            is ExpiredMeeting -> throw UnsupportedOperationException()
            is RejectedMeeting -> throw UnsupportedOperationException()
        }
        meetingJpaRepository.save(meetingEntity)
        return meeting
    }

    override fun saveAllExpired(meetings: Collection<ExpiredMeeting>) {
        meetingJpaRepository.updateStatus(MeetingStatus.EXPIRED, meetings.map { it.meetingId })
    }

    override fun findAllProposedSlots(
        initiatorId: UserId,
        fromDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ): Collection<MeetingSlot> {
        return meetingJpaRepository.findAllProposedSlots(initiatorId, fromDateTime, endDateTime)
    }
}

@Factory
fun meetingFrom(
    meetingIntentEntity: MeetingIntentEntity,
    meetingEntity: MeetingEntity
): Meeting {
    return when (meetingEntity.status!!) {
        MeetingStatus.PROPOSED -> {
            val proposedSlots = meetingEntity.proposedSlots
                .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            ProposedMeeting(
                meetingEntity.id!!,
                meetingIntentEntity.id!!,
                meetingIntentEntity.initiatorId!!,
                Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
                proposedSlots,
                meetingEntity.code!!
            )
        }
        MeetingStatus.CONFIRMED -> ConfirmedMeeting(
            meetingEntity.id!!,
            meetingIntentEntity.initiatorId!!,
            Duration.ofMinutes(meetingIntentEntity.durationMinutes!!),
            meetingEntity.meetingRecipient!!,
            meetingEntity.confirmedSlot!!

        )
        MeetingStatus.REJECTED -> RejectedMeeting(meetingEntity.id!!)
        MeetingStatus.EXPIRED -> ExpiredMeeting(meetingEntity.id!!)
    }
}