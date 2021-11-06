package com.sama.integration.google.calendar.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.usingLaxDateTimePrecision
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.auth.infrastructure.JdbcGoogleAccountRepository
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarEventKey
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.toUserId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JdbcCalendarEventRepository::class, JdbcGoogleAccountRepository::class])
class CalendarEventRepositoryTest : BasePersistenceIT<JdbcCalendarEventRepository>() {

    @Autowired
    lateinit var userRepository: UserJpaRepository

    @Autowired
    lateinit var googleAccountRepository: GoogleAccountRepository

    private lateinit var user: UserEntity
    private lateinit var googleAccount: GoogleAccount

    @BeforeEach
    fun setup() {
        val email = "one@meetsama.com"
        user = userRepository.save(UserEntity(email))
        userRepository.flush()
        googleAccount = googleAccountRepository.save(GoogleAccount.new(user.id!!.toUserId(), email, true))
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `save and find by key`() {
        val now = ZonedDateTime.now()
        val eventKey = GoogleCalendarEventKey(googleAccount.id!!, "default", "event-id")
        val event = CalendarEvent(
            eventKey,
            now, now.plusHours(1), EventData("title", false, 1, "recurring-id", now.minusHours(1)), emptySet()
        )
        underTest.save(event)

        val actual = underTest.find(eventKey)

        assertThat(actual)
            .usingLaxDateTimePrecision()
            .usingRecursiveComparison()
            .isEqualTo(event)
    }

    @Test
    fun `save and find by keys`() {
        val now = ZonedDateTime.now()
        val eventKey = GoogleCalendarEventKey(googleAccount.id!!, "default", "event-id")
        val event = CalendarEvent(
            eventKey,
            now, now.plusHours(1), EventData("title", false, 1, "recurring-id", now.minusHours(1)), emptySet()
        )
        underTest.save(event)

        val actual = underTest.findAll(setOf(eventKey))

        assertThat(actual.first())
            .usingLaxDateTimePrecision()
            .usingRecursiveComparison()
            .isEqualTo(event)
    }

    @Test
    fun `save and find by date range`() {
        val now = ZonedDateTime.now()
        val eventData = EventData("title", false, 1, "recurring-id", now.minusHours(1))
        val eventEndingNow = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-ends-now"),
            now.minusHours(1), now, eventData, emptySet()
        )
        val pastEvent = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-in-the-past"),
            now.minusHours(2), now.minusHours(1), eventData, emptySet()
        )
        val futureEvent = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-in-the-future"),
            now.plusHours(1), now.plusHours(2), eventData, emptySet()
        )
        underTest.saveAll(listOf(eventEndingNow, pastEvent, futureEvent))

        val actual = underTest.findAll(googleAccount.id!!, "default", now, now.plusHours(3))

        assertThat(actual)
            .usingRecursiveComparison()
            .withComparatorForType(
                Comparator.comparing { it.truncatedTo(ChronoUnit.SECONDS) },
                ZonedDateTime::class.java
            )
            .ignoringCollectionOrder()
            .isEqualTo(listOf(eventEndingNow, futureEvent))
    }


    @Test
    fun `save and find by created from & min attendees`() {
        val now = ZonedDateTime.now()
        val eventData = EventData("title", false, 1, "recurring-id", now)
        val eventCreatedNow = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-ends-now"),
            now, now.plusHours(1), eventData.copy(created = now), emptySet()
        )
        val eventCreatedInThePast = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-in-the-past"),
            now, now.plusHours(1), eventData.copy(created = now.minusHours(1)), emptySet()
        )
        val eventCreatedInTheFuture = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-in-the-future"),
            now, now.plusHours(1), eventData.copy(created = now.plusHours(1)), emptySet()
        )
        val eventWithoutCreatedDate = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-no-created-date"),
            now, now.plusHours(1), eventData.copy(created = null), emptySet()
        )
        val eventWithoutAttendees = CalendarEvent(
            GoogleCalendarEventKey(googleAccount.id!!, "default", "event-without-attendees"),
            now, now.plusHours(1), eventData.copy(created = now.plusHours(1), attendeeCount = 0), emptySet()
        )
        underTest.saveAll(
            listOf(
                eventCreatedNow,
                eventCreatedInThePast,
                eventCreatedInTheFuture,
                eventWithoutCreatedDate,
                eventWithoutAttendees
            )
        )

        val actual = underTest.findAll(
            googleAccount.id!!, "default", now, now.plusHours(3),
            createdFrom = now, minAttendeeCount = 1
        )

        assertThat(actual)
            .usingRecursiveComparison()
            .withComparatorForType(
                Comparator.comparing { it.truncatedTo(ChronoUnit.SECONDS) },
                ZonedDateTime::class.java
            )
            .ignoringCollectionOrder()
            .isEqualTo(listOf(eventCreatedNow, eventCreatedInTheFuture, eventWithoutCreatedDate))
    }

}