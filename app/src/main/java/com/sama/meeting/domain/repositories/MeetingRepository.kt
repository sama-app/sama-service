package com.sama.meeting.domain.repositories

import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.aggregates.MeetingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingRepository : JpaRepository<MeetingEntity, MeetingId> {
    @Query("select nextval('sama.meeting_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingId

    fun findByCode(code: MeetingCode): MeetingEntity?
}

fun MeetingRepository.findByCodeOrThrow(code: MeetingCode): MeetingEntity = findByCode(code)
    ?: throw NotFoundException(MeetingEntity::class, "code", code)