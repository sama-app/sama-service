package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.NotFoundException
import com.sama.meeting.domain.Actor
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.meeting.infrastructure.jpa.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        this.timezone = UTC
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
            Duration.ofMinutes(60),
            UserId(1),
            null,
            UTC,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            emptyList(),
            MeetingCode("VGsUTGno"),
            "Meeting title"
        )

        // act
        underTest.save(proposedMeeting)
        val persisted = underTest.findByIdOrThrow(proposedMeeting.meetingId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .isEqualTo(proposedMeeting)
    }

    @Test
    fun `confirmed meeting persistence`() {
        val meetingId = MeetingId(21)
        val meetingCode = MeetingCode("VGsUTGno")
        val meetingTitle = "Meeting title"
        val initiatorId = UserId(1)

        // act
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            initiatorId,
            null,
            UTC,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            emptyList(),
            meetingCode,
            meetingTitle
        )

        underTest.save(proposedMeeting)

        val confirmedMeeting = ConfirmedMeeting(
            meetingId, initiatorId, Duration.ofMinutes(60),
            UserRecipient.of(UserId(2), "test@meetsama.com"),
            MeetingSlot(
                ZonedDateTime.now(clock).plusHours(3),
                ZonedDateTime.now(clock).plusHours(4)
            ),
            meetingTitle
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
            Duration.ofMinutes(60),
            initiatorId,
            null,
            UTC,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            emptyList(),
            meetingCode,
            "Meeting title"
        )

        underTest.save(proposedMeeting)

        // act
        val persisted = underTest.findByCodeOrThrow(meetingCode)
        val persistedForUpdate = underTest.findByCodeOrThrow(meetingCode, true)

        // verify
        assertThat(persisted).isEqualTo(proposedMeeting)
        assertThat(persistedForUpdate).isEqualTo(proposedMeeting)
    }

    @Test
    fun `find by code not found`() {
        assertThrows<NotFoundException> {
            underTest.findByCodeOrThrow(MeetingCode("non existant"))
        }

        assertThrows<NotFoundException> {
            underTest.findByCodeOrThrow(MeetingCode("non existant"), true)
        }
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
            Duration.ofMinutes(60),
            initiatorId,
            null,
            UTC,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(30),
                    ZonedDateTime.now(clock).plusMinutes(30)
                ),
                expected
            ),
            emptyList(),
            MeetingCode("VGsUTGno"),
            "Meeting title"
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
            Duration.ofMinutes(60),
            UserId(1),
            null,
            UTC,
            Actor.RECIPIENT,
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
            emptyList(),
            MeetingCode("one"),
            "Meeting title"
        )

        val expiringMeetingId = MeetingId(21)
        val expiringMeeting = ProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
            null,
            null,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(2),
                    ZonedDateTime.now(clock).minusHours(1)
                ),
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(61),
                    ZonedDateTime.now(clock).minusMinutes(1)
                )
            ),
            emptyList(),
            MeetingCode("two"),
            "Meeting title"
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
            Duration.ofMinutes(60),
            UserId(1),
            null,
            UTC,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(3),
                    ZonedDateTime.now(clock).minusHours(4)
                )
            ),
            emptyList(),
            MeetingCode("VGsUTGno"),
            "Meeting title"
        )
        underTest.save(expiringMeeting)

        // act
        underTest.saveAllExpired(listOf(ExpiredMeeting(expiringMeetingId)))

        // verify
        val result = underTest.findByIdOrThrow(expiringMeetingId)
        assertThat(result.status).isEqualTo(MeetingStatus.EXPIRED)
    }
}