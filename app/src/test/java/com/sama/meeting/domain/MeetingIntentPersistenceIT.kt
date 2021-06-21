package com.sama.meeting.domain

import com.sama.common.BasePersistenceIT
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertNotEquals

class MeetingIntentPersistenceIT: BasePersistenceIT<MeetingIntentRepository>() {

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
}