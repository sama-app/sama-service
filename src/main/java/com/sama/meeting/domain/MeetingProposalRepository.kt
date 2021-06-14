package com.sama.meeting.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingProposalRepository : JpaRepository<MeetingProposalEntity, MeetingProposalId> {
    @Query("select nextval('sama.meeting_proposal_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingProposalId

    fun findByCodeAndStatus(code: MeetingCode, status: MeetingProposalStatus): MeetingProposalEntity?
}