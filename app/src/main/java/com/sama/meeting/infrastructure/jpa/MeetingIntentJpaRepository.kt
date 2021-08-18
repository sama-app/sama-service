package com.sama.meeting.infrastructure.jpa

import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.users.domain.UserId
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingIntentJpaRepository : JpaRepository<MeetingIntentEntity, Long> {
    @Query("select nextval('sama.meeting_intent_id_seq')", nativeQuery = true)
    fun nextIdentity(): Long

    fun findByCode(code: UUID): MeetingIntentEntity?

    fun existsByIdAndInitiatorId(meetingIntentId: Long, initiatorId: Long): Boolean

    fun existsByCodeAndInitiatorId(code: UUID, initiatorId: Long): Boolean
}

fun MeetingIntentJpaRepository.findByCodeOrThrow(code: UUID): MeetingIntentEntity = findByCode(code)
    ?: throw NotFoundException(MeetingIntentEntity::class, "code", code)