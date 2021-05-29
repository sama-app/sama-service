package com.sama.calendar.domain

import com.sama.users.domain.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MeetingIntentRepository : JpaRepository<MeetingIntentEntity, MeetingIntentId> {
    @Query("select nextval('sama.meeting_intent_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingIntentId

    fun existsByIdAndInitiatorId(meetingIntentId: MeetingIntentId, initiatorId: UserId): Boolean
}