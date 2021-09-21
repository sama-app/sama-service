package com.sama.meeting.infrastructure

import com.sama.common.NotFoundException
import com.sama.meeting.domain.*
import com.sama.meeting.infrastructure.jpa.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class MeetingIntentRepositoryImpl(private val meetingIntentJpaRepository: MeetingIntentJpaRepository) :
    MeetingIntentRepository {

    override fun nextIdentity(): MeetingIntentId {
        return meetingIntentJpaRepository.nextIdentity().toMeetingIntentId()
    }

    override fun findByCodeOrThrow(code: MeetingIntentCode): MeetingIntent {
        val meetingIntentEntity = (meetingIntentJpaRepository.findByCode(code.code)
            ?: throw NotFoundException(MeetingIntentEntity::class, "code", code))

        return meetingIntentEntity.toDomainObject()
    }

    override fun existsByCodeAndInitiatorId(code: MeetingIntentCode, initiatorId: UserId): Boolean {
        return meetingIntentJpaRepository.existsByCodeAndInitiatorId(code.code, initiatorId.id)
    }

    override fun save(meetingIntent: MeetingIntent): MeetingIntent {
        var meetingIntentEntity = MeetingIntentEntity.new(meetingIntent)
        meetingIntentEntity = meetingIntentJpaRepository.save(meetingIntentEntity)
        return meetingIntentEntity.toDomainObject()
    }

    fun MeetingIntentEntity.toDomainObject(): MeetingIntent {
        return MeetingIntent(
            id!!.toMeetingIntentId(),
            initiatorId!!.toUserId(),
            Duration.ofMinutes(durationMinutes!!),
            recipientId?.toUserId(),
            timezone!!,
            suggestedSlots.map { MeetingSlot(it.startDateTime, it.endDateTime) },
            code?.toMeetingIntentCode()
        )
    }
}

fun Long.toMeetingIntentId(): MeetingIntentId {
    return MeetingIntentId(this)
}

fun UUID.toMeetingIntentCode(): MeetingIntentCode {
    return MeetingIntentCode(this)
}