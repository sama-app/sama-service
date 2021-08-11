package com.sama.meeting.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import com.sama.meeting.domain.repositories.findByCodeOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals

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

        val toPersist = MeetingIntentEntity.new(meetingIntent)

        // act
        underTest.save(toPersist)
        val persisted = underTest.findByIdOrThrow(meetingIntentId)

        // verify
        assertThat(persisted).usingRecursiveComparison()
            .ignoringFields("suggestedSlots.id") // db generated
            .isEqualTo(toPersist)
        assertThat(persisted.suggestedSlots).allMatch { it.id != null }
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

        val persisted = underTest.save(MeetingIntentEntity.new(meetingIntent))

        assertThat(persisted.code).isNotNull()

        val fetchedByCode = underTest.findByCodeOrThrow(persisted.code!!)
        assertThat(persisted.id).isEqualTo(fetchedByCode.id)
    }
}