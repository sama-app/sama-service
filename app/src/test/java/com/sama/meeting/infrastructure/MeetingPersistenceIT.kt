package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals

@ContextConfiguration(classes = [MeetingRepositoryImpl::class])
class MeetingPersistenceIT : BasePersistenceIT<MeetingRepository>() {

    @Autowired
    private lateinit var meetingIntentRepository: MeetingIntentJpaRepository

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
    fun `proposed meeting persistance`() {
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

        // act
        underTest.save(proposedMeeting)
        val persisted = underTest.findByIdOrThrow(proposedMeeting.meetingId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .isEqualTo(proposedMeeting)
    }

    @Test
    fun `confirmed meeting persistance`() {
        val meetingId = 21L
        val meetingCode = "meeting-code"
        val initiatorId = 1L

        // act
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            initiatorId,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            meetingCode
        )

        underTest.save(proposedMeeting)

        val confirmedMeeting = ConfirmedMeeting(
            meetingId, initiatorId, Duration.ofMinutes(60),
            MeetingRecipient(2L, "test@meetsama.com"),
            MeetingSlot(
                ZonedDateTime.now(clock).plusHours(3),
                ZonedDateTime.now(clock).plusHours(4)
            )
        )

        // act
        underTest.save(confirmedMeeting)
        val persisted = underTest.findByIdOrThrow(confirmedMeeting.meetingId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .isEqualTo(confirmedMeeting)
    }

    @Test
    fun `find by code`() {
        val meetingId = 21L
        val meetingCode = "meeting-code"
        val initiatorId = 1L

        // act
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            initiatorId,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            meetingCode
        )

        underTest.save(proposedMeeting)

        // act
        val persisted = underTest.findByCodeOrThrow(meetingCode)

        // verify
        assertThat(persisted.meetingId).isEqualTo(meetingId)
    }

    @Test
    fun `find future proposed slots`() {
        val validMeetingId = 22L
        val initiatorId = 1L

        val expected = MeetingSlot(
            ZonedDateTime.now(clock).plusDays(1).plusMinutes(1),
            ZonedDateTime.now(clock).plusDays(1).plusMinutes(61)
        )

        val proposedMeeting = ProposedMeeting(
            validMeetingId,
            meetingIntentId,
            initiatorId,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(30),
                    ZonedDateTime.now(clock).plusMinutes(30)
                ),
                expected
            ),
            "code"
        )
        underTest.save(proposedMeeting)

        // act
        val actual = underTest.findAllProposedSlots(
            initiatorId,
            ZonedDateTime.now(clock).plusDays(1),
            ZonedDateTime.now(clock).plusDays(2)
        )

        assertThat(actual).containsExactly(expected)
    }

    @Test
    fun `find expiring`() {
        val validMeetingId = 22L
        val validMeeting = ProposedMeeting(
            validMeetingId,
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(30),
                    ZonedDateTime.now(clock).plusMinutes(30)
                ),
                MeetingSlot(
                    ZonedDateTime.now(clock).plusMinutes(1),
                    ZonedDateTime.now(clock).plusMinutes(61)
                )
            ),
            "meeting-code-1"
        )

        val expiringMeetingId = 21L
        val expiringMeeting = ProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(2),
                    ZonedDateTime.now(clock).minusHours(1)
                ),
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(30),
                    ZonedDateTime.now(clock).plusMinutes(30)
                )
            ),
            "meeting-code-2"
        )
        underTest.save(validMeeting)
        underTest.save(expiringMeeting)

        // act
        val result = underTest.findAllExpiring(ZonedDateTime.now(clock))

        // verify
        assertThat(result).containsExactly(ExpiredMeeting(expiringMeetingId))
    }

    @Test
    fun `save all expired`() {
        val expiringMeetingId = 21L
        val expiringMeeting = ProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(3),
                    ZonedDateTime.now(clock).minusHours(4)
                )
            ),
            "meeting-code"
        )
        underTest.save(expiringMeeting)

        // act
        underTest.saveAllExpired(listOf(ExpiredMeeting(expiringMeetingId)))

        // verify
        val result = underTest.findByIdOrThrow(expiringMeetingId)
        assertThat(result.status).isEqualTo(MeetingStatus.EXPIRED)
    }
}