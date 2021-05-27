package com.sama.calendar.domain

import com.sama.users.domain.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingRepository : JpaRepository<MeetingEntity, MeetingId> {
    @Query("select nextval('sama.meeting_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingId

    @Query("select nextval('sama.meeting_slot_id_seq')", nativeQuery = true)
    fun nextSlotIdentity(): SlotId

    fun findByCode(code: MeetingCode): MeetingEntity?

    fun existsByIdAndInitiatorId(meetingId: MeetingId, initiator: UserId): Boolean
}