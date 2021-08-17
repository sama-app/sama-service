package com.sama.meeting.domain.repositories

import com.sama.common.DomainRepository
import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.users.domain.UserId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@DomainRepository
@Repository
interface MeetingRepository : JpaRepository<MeetingEntity, MeetingId> {
    @Query("select nextval('sama.meeting_id_seq')", nativeQuery = true)
    fun nextIdentity(): MeetingId

    fun findByCode(code: MeetingCode): MeetingEntity?

    @Query(
        "SELECT m.id " +
                "FROM MeetingEntity m JOIN MeetingProposedSlotEntity ps ON m.id = ps.meetingId " +
                "WHERE m.status = 'PROPOSED' GROUP BY m.id " +
                "HAVING max(ps.startDateTime) < :expiryDateTime"
    )
    fun findAllIdsExpiring(@Param("expiryDateTime") expiryDateTime: ZonedDateTime): Collection<MeetingId>

    @Query(
        "SELECT new com.sama.meeting.domain.MeetingSlot(mps.startDateTime, mps.endDateTime) " +
                "FROM MeetingEntity m JOIN MeetingIntentEntity mi ON m.meetingIntentId = mi.id " +
                "JOIN MeetingProposedSlotEntity mps ON m.id = mps.meetingId " +
                "WHERE mi.initiatorId = :initiatorId AND " +
                "   mps.startDateTime > :fromDateTime AND " +
                "   mps.startDateTime < :endDateTime"
    )
    fun findAllProposedSlots(initiatorId: UserId, fromDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<MeetingSlot>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MeetingEntity m SET status = :status WHERE id IN :ids")
    fun updateStatus(status: MeetingStatus, ids: Collection<MeetingId>)
}

fun MeetingRepository.findByCodeOrThrow(code: MeetingCode): MeetingEntity = findByCode(code)
    ?: throw NotFoundException(MeetingEntity::class, "code", code)