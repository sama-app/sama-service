package com.sama.meeting.infrastructure.jpa

import com.sama.common.NotFoundException
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import java.util.Optional
import javax.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MeetingJpaRepository : JpaRepository<MeetingEntity, Long> {
    @Query("select nextval('sama.meeting_id_seq')", nativeQuery = true)
    fun nextIdentity(): Long

    override fun findById(id: Long): Optional<MeetingEntity>

    fun findByCode(code: String): MeetingEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByCode(code: String): MeetingEntity?

    @Query(
        "SELECT m.id " +
                "FROM MeetingEntity m JOIN MeetingProposedSlotEntity ps ON m.id = ps.meetingId " +
                "WHERE m.status = 'PROPOSED' GROUP BY m.id " +
                "HAVING max(ps.startDateTime) < :expiryDateTime"
    )
    fun findAllIdsExpiring(@Param("expiryDateTime") expiryDateTime: ZonedDateTime): Collection<Long>

    @Query(
        "SELECT new com.sama.meeting.domain.MeetingSlot(mps.startDateTime, mps.endDateTime) " +
                "FROM MeetingEntity m JOIN MeetingIntentEntity mi ON m.meetingIntentId = mi.id " +
                "JOIN MeetingProposedSlotEntity mps ON m.id = mps.meetingId " +
                "WHERE mi.initiatorId = :initiatorId AND " +
                "   mps.startDateTime > :fromDateTime AND " +
                "   mps.startDateTime < :endDateTime"
    )
    fun findAllProposedSlots(
        initiatorId: UserId,
        fromDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ): Collection<MeetingSlot>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MeetingEntity m SET status = :status WHERE id IN :ids")
    fun updateStatus(status: MeetingStatus, ids: Collection<Long>)
}

fun MeetingJpaRepository.findByCodeOrThrow(code: String): MeetingEntity = findByCode(code)
    ?: throw NotFoundException(MeetingEntity::class, "code", code)

fun MeetingJpaRepository.findLockedByCodeOrThrow(code: String): MeetingEntity = findFirstByCode(code)
    ?: throw NotFoundException(MeetingEntity::class, "code", code)