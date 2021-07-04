package com.sama.meeting.domain.repositories

import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.aggregates.MeetingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

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

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MeetingEntity m SET status = :status WHERE id IN :ids")
    fun updateStatus(status: MeetingStatus, ids: Collection<MeetingId>)
}

fun MeetingRepository.findByCodeOrThrow(code: MeetingCode): MeetingEntity = findByCode(code)
    ?: throw NotFoundException(MeetingEntity::class, "code", code)