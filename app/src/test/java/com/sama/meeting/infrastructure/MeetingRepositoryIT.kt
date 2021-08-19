package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingRecipient
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.infrastructure.jpa.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [MeetingRepositoryImpl::class])
class MeetingRepositoryIT : BasePersistenceIT<MeetingRepository>() {

    @Autowired
    private lateinit var meetingIntentRepository: MeetingIntentJpaRepository

    // common
    private val meetingIntentId = MeetingIntentId(11)
    private val meetingIntentEntity = MeetingIntentEntity().apply {
        this.id = meetingIntentId.id
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
    fun `proposed meeting persistence`() {
        val meetingId = MeetingId(21)
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            UserId(1),
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            MeetingCode("VGsUTGno")
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
        val meetingId = MeetingId(21)
        val meetingCode = MeetingCode("VGsUTGno")
        val initiatorId = UserId(1)

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
            MeetingRecipient(UserId(2), "test@meetsama.com"),
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
        val meetingId = MeetingId(21)
        val meetingCode = MeetingCode("VGsUTGno")
        val initiatorId = UserId(1)

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
        val validMeetingId = MeetingId(21)
        val initiatorId = UserId(1)

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
            MeetingCode("VGsUTGno")
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
        val validMeetingId = MeetingId(22)
        val validMeeting = ProposedMeeting(
            validMeetingId,
            meetingIntentId,
            UserId(1),
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
            MeetingCode("one")
        )

        val expiringMeetingId = MeetingId(21)
        val expiringMeeting = ProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            UserId(1),
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
            MeetingCode("two")
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
        val expiringMeetingId = MeetingId(21)
        val expiringMeeting = ProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            UserId(1),
            Duration.ofMinutes(60),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(3),
                    ZonedDateTime.now(clock).minusHours(4)
                )
            ),
            MeetingCode("VGsUTGno")
        )
        underTest.save(expiringMeeting)

        // act
        underTest.saveAllExpired(listOf(ExpiredMeeting(expiringMeetingId)))

        // verify
        val result = underTest.findByIdOrThrow(expiringMeetingId)
        assertThat(result.status).isEqualTo(MeetingStatus.EXPIRED)
    }
}