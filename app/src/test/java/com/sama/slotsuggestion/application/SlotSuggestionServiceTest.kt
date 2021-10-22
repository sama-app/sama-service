package com.sama.slotsuggestion.application

import com.sama.calendar.application.EventDTO
import com.sama.common.datesUtil
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.application.CalendarEventDTO
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.SyncGoogleCalendarService
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.EventData
import com.sama.meeting.application.MeetingDataService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.User
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.WorkingHours
import com.sama.users.domain.UserId
import java.time.Clock
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension


interface Persona {
    fun pastBlocks(): Collection<CalendarEventDTO>
    fun futureBlocks(): Collection<CalendarEventDTO>
    fun workingHours(): Map<DayOfWeek, WorkingHours>
}

// 2021-05-31 00:00 UTC, Monday
private val fixedDate = LocalDate.of(2021, 5, 31)

private val nineToFive = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    .associateWith {
        WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
    }

private val userId = UserId(1)
private val googleAccountId = GoogleAccountId(1)
val nonCalendarUser = object : Persona {
    override fun pastBlocks(): Collection<CalendarEventDTO> = emptyList()

    override fun futureBlocks(): Collection<CalendarEventDTO> = emptyList()

    override fun workingHours(): Map<DayOfWeek, WorkingHours> = nineToFive
}

val fullyBlockedCalendarUser = object : Persona {
    override fun pastBlocks(): Collection<CalendarEventDTO> = emptyList()

    override fun futureBlocks(): Collection<CalendarEventDTO> = fixedDate
        .datesUtil(fixedDate.plusDays(14))
        .map {
            CalendarEventDTO(
                GoogleAccountPublicId(UUID.randomUUID()), "primary", "eventID",
                ZonedDateTime.of(it, LocalTime.MIN, UTC),
                ZonedDateTime.of(it, LocalTime.MAX, UTC),
                EventData(allDay = false, attendeeCount = 2),
                AggregatedData(recurrenceCount = 1)
            )
        }
        .toList()


    override fun workingHours(): Map<DayOfWeek, WorkingHours> = nineToFive
}


@TestConfiguration
class SlotSuggestionServiceTestConfiguration {
    @Bean
    @Primary
    fun clock(): Clock {
        // Force JVM timezone to UTC so that the JDBC driver interpret timezones consistently
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        // 2021-06-02 00:00 UTC, Monday
        return Clock.fixed(fixedDate.atStartOfDay().toInstant(UTC), ZoneId.of("UTC"))
    }

    @Bean
    fun heatMapConfiguration(): HeatMapConfiguration {
        return HeatMapConfiguration(15, 90, 14)
    }
}


@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        HeatMapSlotSuggestionService::class,
        HeatMapService::class,
        SlotSuggestionServiceTestConfiguration::class,
        SyncGoogleCalendarService::class,
    ]
)
class SlotSuggestionServiceTest {

    @MockBean
    lateinit var meetingDataService: MeetingDataService

    @MockBean
    lateinit var userRepository: UserRepository

    @MockBean
    lateinit var googleCalendarService: GoogleCalendarService

    @Autowired
    lateinit var config: HeatMapConfiguration

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var underTest: HeatMapSlotSuggestionService

    @Test
    fun `non calendar user gets two suggestions today and one next week`() {
        setupPersona(nonCalendarUser)

        val suggestions =
            underTest.suggestSlots(userId, SlotSuggestionRequest(Duration.ofHours(1), 3, ZoneId.of("UTC")))

        assertThat(suggestions.suggestions).hasSize(3)

        val (first, second, third) = suggestions.suggestions

        assertThat(first.startDateTime).isEqualTo("2021-05-31T09:00:00Z")
        assertThat(second.startDateTime).isEqualTo("2021-05-31T13:00:00Z")
        assertThat(third.startDateTime).isEqualTo("2021-06-07T10:00:00Z")
    }

    @Test
    fun `fully blocked calendar user gets two suggestions today and one next week`() {
        setupPersona(fullyBlockedCalendarUser)

        val suggestions =
            underTest.suggestSlots(userId, SlotSuggestionRequest(Duration.ofHours(1), 3, ZoneId.of("UTC")))

        assertThat(suggestions.suggestions).hasSize(3)

        val (first, second, third) = suggestions.suggestions

        assertThat(first.startDateTime).isEqualTo("2021-05-31T09:00:00Z")
        assertThat(second.startDateTime).isEqualTo("2021-05-31T13:00:00Z")
        assertThat(third.startDateTime).isEqualTo("2021-06-07T10:00:00Z")
    }

    private fun setupPersona(persona: Persona) {
        whenever(userRepository.findById(userId))
            .thenReturn(User(userId, ZoneId.of("UTC"), persona.workingHours()))

        val now = ZonedDateTime.now(clock)

        given(
            googleCalendarService.findEvents(
                userId,
                now.minusDays(config.historicalDays),
                now.plusDays(config.futureDays),
            )
        ).willReturn(persona.pastBlocks().plus(persona.futureBlocks()))
    }
}
