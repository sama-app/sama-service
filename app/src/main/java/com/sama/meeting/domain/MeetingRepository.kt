package com.sama.meeting.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import org.springframework.data.repository.Repository

@DomainRepository
interface MeetingRepository : Repository<Meeting, MeetingId> {
    fun nextIdentity(): MeetingId

    fun findByIdOrThrow(meetingId: MeetingId): Meeting

    fun findByCodeOrThrow(code: MeetingCode): Meeting

    fun findAllExpiring(expiryDateTime: ZonedDateTime): Collection<ExpiredMeeting>

    fun findAllProposedSlots(initiatorId: UserId, fromDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<MeetingSlot>

    fun save(meeting: Meeting): Meeting

    fun saveAllExpired(meetings: Collection<ExpiredMeeting>)
}