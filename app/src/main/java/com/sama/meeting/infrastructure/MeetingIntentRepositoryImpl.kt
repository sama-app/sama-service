package com.sama.meeting.infrastructure

import com.sama.common.NotFoundException
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MeetingIntentRepositoryImpl(private val meetingIntentJpaRepository: MeetingIntentJpaRepository) :
    MeetingIntentRepository {

    override fun nextIdentity(): MeetingIntentId {
        return meetingIntentJpaRepository.nextIdentity()
    }

    override fun findByCodeOrThrow(code: MeetingIntentCode): MeetingIntent {
        val meetingIntentEntity = (meetingIntentJpaRepository.findByCode(code)
            ?: throw NotFoundException(MeetingIntentEntity::class, "code", code))

        return meetingIntentEntity.toDomainObject()
    }

    override fun existsByIdAndInitiatorId(meetingIntentId: MeetingIntentId, initiatorId: UserId): Boolean {
        return meetingIntentJpaRepository.existsByIdAndInitiatorId(meetingIntentId, initiatorId)
    }

    override fun existsByCodeAndInitiatorId(code: MeetingIntentCode, initiatorId: UserId): Boolean {
        return meetingIntentJpaRepository.existsByCodeAndInitiatorId(code, initiatorId)
    }

    override fun save(meetingIntent: MeetingIntent): MeetingIntent {
        var meetingIntentEntity = MeetingIntentEntity.new(meetingIntent)
        meetingIntentEntity = meetingIntentJpaRepository.save(meetingIntentEntity)
        return meetingIntentEntity.toDomainObject()
    }

    fun MeetingIntentEntity.toDomainObject(): MeetingIntent {
        return MeetingIntent(
            id!!,
            initiatorId!!,
            Duration.ofMinutes(durationMinutes!!),
            timezone!!,
            suggestedSlots.map { MeetingSlot(it.startDateTime, it.endDateTime) },
            code
        )
    }
}