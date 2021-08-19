package com.sama.meeting.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface MeetingIntentRepository : Repository<MeetingIntent, MeetingIntentId> {
    fun nextIdentity(): MeetingIntentId

    fun findByCodeOrThrow(code: MeetingIntentCode): MeetingIntent

    fun existsByCodeAndInitiatorId(code: MeetingIntentCode, initiatorId: UserId): Boolean

    fun save(meetingIntent: MeetingIntent): MeetingIntent
}