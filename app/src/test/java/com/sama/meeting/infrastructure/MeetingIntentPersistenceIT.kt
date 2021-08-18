package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentRepository
import com.sama.meeting.domain.MeetingSlot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals

@ContextConfiguration(classes = [MeetingIntentRepositoryImpl::class])
class MeetingIntentPersistenceIT : BasePersistenceIT<MeetingIntentRepository>() {

    @Test
    fun `next identity`() {
        val one = underTest.nextIdentity()
        val two = underTest.nextIdentity()
        assertNotEquals(one, two)
    }

    @Test
    fun `meeting intent persists from domain entity`() {
        val meetingIntentId = 11L
        val meetingIntent = MeetingIntent(
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            ZoneId.of("Europe/Rome"),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            )
        )

        // act
        val persisted = underTest.save(meetingIntent)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .ignoringFields("code") // db generated
            .isEqualTo(meetingIntent)
        assertThat(persisted.code).isNotNull
    }

    @Test
    fun `meeting intent find by code`() {
        val meetingIntentId = 11L
        val meetingIntent = MeetingIntent(
            meetingIntentId,
            1L,
            Duration.ofMinutes(60),
            ZoneId.of("Europe/Rome"),
            listOf(
                MeetingSlot(
                    ZonedDateTime.now(clock).plusHours(3),
                    ZonedDateTime.now(clock).plusHours(4)
                )
            )
        )

        val persisted = underTest.save(meetingIntent)
        assertThat(persisted.code).isNotNull

        val fetchedByCode = underTest.findByCodeOrThrow(persisted.code!!)
        assertThat(meetingIntent.meetingIntentId).isEqualTo(fetchedByCode.meetingIntentId)
    }
}