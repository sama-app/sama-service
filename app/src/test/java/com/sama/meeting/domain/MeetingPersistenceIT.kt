package com.sama.meeting.domain

import com.sama.AppTestConfiguration
import com.sama.PersistenceConfiguration
import com.sama.common.BasePersistenceIT
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import com.sama.meeting.domain.repositories.MeetingRepository
import liquibase.pro.packaged.it
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import kotlin.test.assertNotEquals


class MeetingPersistenceIT : BasePersistenceIT<MeetingRepository>() {

    @Autowired
    private lateinit var meetingIntentRepository: MeetingIntentRepository

    // common
    private val meetingIntentId = 11L
    private val meetingIntentEntity = MeetingIntentEntity().apply {
        this.id = meetingIntentId
        this.initiatorId = 1L
        this.durationMinutes = 60
        this.timezone = ZoneId.systemDefault()
        this.createdAt = Instant.now()
        this.updatedAt = Instant.now()
    }

    @BeforeEach
    fun setupMeetingIntent() {
        meetingIntentRepository.save(meetingIntentEntity)
    }

    @Test
    fun `next identity`() {
        val one = underTest.nextIdentity()
        val two = underTest.nextIdentity()
        assertNotEquals(one, two)
    }

    @Test
    fun `meeting persists from proposed domain entity`() {
        val meetingId = 21L
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            "meeting-code"
        )

        val toPersist = MeetingEntity.new(proposedMeeting)

        // act
        underTest.save(toPersist)
        val persisted = underTest.findByIdOrThrow(meetingId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .ignoringFields("proposedSlots.id") // db generated
            .isEqualTo(toPersist)
        assertThat(persisted.proposedSlots).allMatch { it.id != null }

        assertThat(meetingFrom(meetingIntentEntity, persisted).getOrNull())
            .usingRecursiveComparison()
            .isEqualTo(proposedMeeting)
    }

    @Test
    fun `meeting persists from applied domain entity changes`() {
        val meetingId = 21L
        val meetingCode = "meeting-code"

        val entity = MeetingEntity()
        entity.id = meetingId
        entity.code = meetingCode
        entity.meetingIntentId = meetingIntentId
        entity.status = MeetingStatus.PROPOSED
        entity.createdAt = Instant.now()
        entity.updatedAt = Instant.now()
        underTest.save(entity)

        val confirmedMeeting = ConfirmedMeeting(
            meetingId, 1L, Duration.ofMinutes(60),
            MeetingRecipient(2L, "test@yoursama.com"),
            MeetingSlot(
                ZonedDateTime.now(clock).plusHours(3),
                ZonedDateTime.now(clock).plusHours(4)
            )
        )
        val toPersist = entity.applyChanges(confirmedMeeting)

        // act
        underTest.save(toPersist)
        val persisted = underTest.findByIdOrThrow(meetingId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .isEqualTo(toPersist)

        assertThat(meetingFrom(meetingIntentEntity, persisted).getOrNull())
            .usingRecursiveComparison()
            .isEqualTo(confirmedMeeting)
    }

    @Test
    fun `find by code`() {
        val meetingId = 21L
        val meetingCode = "meeting-code"

        val entity = MeetingEntity()
        entity.id = meetingId
        entity.code = meetingCode
        entity.meetingIntentId = meetingIntentId
        entity.status = MeetingStatus.PROPOSED
        entity.createdAt = Instant.now()
        entity.updatedAt = Instant.now()
        underTest.save(entity)

        // act
        val persisted = underTest.findByCode(meetingCode)

        // verify
        assertThat(persisted!!.id).isEqualTo(meetingId)
    }
}