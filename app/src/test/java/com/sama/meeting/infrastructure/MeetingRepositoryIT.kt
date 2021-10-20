package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.NotFoundException
import com.sama.meeting.domain.Actor
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingPreferences
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingStatus
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.meeting.domain.SamaSamaProposedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.meeting.infrastructure.jpa.MeetingIntentEntity
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.Instant
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
        val proposedMeeting = SamaNonSamaProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            MeetingCode("VGsUTGno"),
            "Meeting title",
            MeetingPreferences.default(),
            null
        )

        // act
        underTest.save(proposedMeeting)
        val persisted = underTest.findByIdOrThrow(proposedMeeting.meetingId) as SamaNonSamaProposedMeeting

        // verify
        assertThat(persisted)
            .usingRecursiveComparison()
            .ignoringFields("createdAt")
            .isEqualTo(proposedMeeting)
        assertThat(persisted.createdAt).isNotNull()
    }

    @Test
    fun `sama-sama proposed meeting persistence`() {
        val meetingId = MeetingId(21)
        val proposedMeeting = SamaSamaProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
            UserId(2),
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            emptyList(),
            MeetingCode("VGsUTGno"),
            "Meeting title",
            MeetingPreferences.default()
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
        val proposedMeeting = SamaNonSamaProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            initiatorId,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            meetingCode,
            meetingTitle,
            MeetingPreferences.default(),
            null
        )

        underTest.save(proposedMeeting)

        val confirmedMeeting = ConfirmedMeeting(
            meetingId, meetingCode, initiatorId, UserRecipient.of(UserId(2)),
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
        val proposedMeeting = SamaNonSamaProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            initiatorId,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            meetingCode,
            "Meeting title",
            MeetingPreferences.default(),
            null
        )

        underTest.save(proposedMeeting)

        // act
        val persisted = underTest.findByCodeOrThrow(meetingCode) as SamaNonSamaProposedMeeting

        // verify
        assertThat(persisted)
            .usingRecursiveComparison()
            .ignoringFields("createdAt")
            .isEqualTo(proposedMeeting)
        assertThat(persisted.createdAt).isNotNull()
    }

    @Test
    fun `find sama-sama by code`() {
        val meetingId = MeetingId(21)
        val meetingCode = MeetingCode("VGsUTGno")
        val initiatorId = UserId(1)
        val recipientId = UserId(2)

        // act
        val proposedMeeting = SamaSamaProposedMeeting(
            meetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            initiatorId,
            recipientId,
            Actor.RECIPIENT,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            ),
            emptyList(),
            meetingCode,
            "Meeting title",
            MeetingPreferences.default()
        )

        underTest.save(proposedMeeting)

        // act
        val persisted = underTest.findByCodeOrThrow(meetingCode)

        // verify
        assertThat(persisted).isEqualTo(proposedMeeting)
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

        val proposedMeeting = SamaNonSamaProposedMeeting(
            validMeetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            initiatorId,
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusMinutes(30),
                    ZonedDateTime.now(clock).plusMinutes(30)
                ),
                expected
            ),
            MeetingCode("VGsUTGno"),
            "Meeting title",
            MeetingPreferences.default(),
            null
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
        val validMeeting = SamaNonSamaProposedMeeting(
            validMeetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
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
            MeetingCode("one"),
            "Meeting title",
            MeetingPreferences.default(),
            null
        )

        val expiringMeetingId = MeetingId(21)
        val expiringMeeting = SamaNonSamaProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
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
            MeetingCode("two"),
            "Meeting title",
            MeetingPreferences.default(),
            null
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
        val expiringMeeting = SamaNonSamaProposedMeeting(
            expiringMeetingId,
            meetingIntentId,
            Duration.ofMinutes(60),
            UserId(1),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).minusHours(4),
                    ZonedDateTime.now(clock).minusHours(3)
                )
            ),
            MeetingCode("VGsUTGno"),
            "Meeting title",
            MeetingPreferences.default(),
            null
        )
        underTest.save(expiringMeeting)

        // act
        underTest.saveAllExpired(listOf(ExpiredMeeting(expiringMeetingId)))

        // verify
        val result = underTest.findByIdOrThrow(expiringMeetingId)
        assertThat(result.status).isEqualTo(MeetingStatus.EXPIRED)
    }
}