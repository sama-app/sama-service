package com.sama.meeting.domain.repositories

import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingProposalId
import com.sama.meeting.domain.aggregates.MeetingProposalEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingProposalRepository : JpaRepository<MeetingProposalEntity, MeetingProposalId> {
    @Query("select nextval('sama.meeting_proposal_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingProposalId

    fun findByCode(code: MeetingCode): MeetingProposalEntity?
}

fun MeetingProposalRepository.findByCodeOrThrow(code: MeetingCode): MeetingProposalEntity = findByCode(code)
    ?: throw NotFoundException(MeetingProposalEntity::class, "code", code)