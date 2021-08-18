package com.sama.meeting.infrastructure.jpa

import com.sama.common.DomainRepository
import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.users.domain.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingIntentJpaRepository : JpaRepository<MeetingIntentEntity, MeetingIntentId> {
    @Query("select nextval('sama.meeting_intent_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingIntentId

    fun findByCode(code: MeetingIntentCode): MeetingIntentEntity?

    fun existsByIdAndInitiatorId(meetingIntentId: MeetingIntentId, initiatorId: UserId): Boolean

    fun existsByCodeAndInitiatorId(code: MeetingIntentCode, initiatorId: UserId): Boolean
}

fun MeetingIntentJpaRepository.findByCodeOrThrow(code: MeetingIntentCode): MeetingIntentEntity = findByCode(code)
    ?: throw NotFoundException(MeetingIntentEntity::class, "code", code)