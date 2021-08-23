package com.sama.slotsuggestion.application

import com.sama.common.datesUtil
import com.sama.meeting.application.MeetingDataService
import com.sama.slotsuggestion.configuration.HeatMapConfiguration
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.BlockRepository
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
    fun pastBlocks(): Collection<Block>
    fun futureBlocks(): Collection<Block>
    fun workingHours(): Map<DayOfWeek, WorkingHours>
}

// 2021-05-31 00:00 UTC, Monday
private val fixedDate = LocalDate.of(2021, 5, 31)

private val nineToFive = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    .associateWith {
        WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0))
    }

private val userId = UserId(1)
val nonCalendarUser = object : Persona {
    override fun pastBlocks(): Collection<Block> = emptyList()

    override fun futureBlocks(): Collection<Block> = emptyList()

    override fun workingHours(): Map<DayOfWeek, WorkingHours> = nineToFive
}

val fullyBlockedCalendarUser = object : Persona {
    override fun pastBlocks(): Collection<Block> = emptyList()

    override fun futureBlocks(): Collection<Block> = fixedDate
        .datesUtil(fixedDate.plusDays(14))
        .map {
            Block(
                ZonedDateTime.of(it, LocalTime.MIN, UTC),
                ZonedDateTime.of(it, LocalTime.MAX, UTC),
                allDay = false,
                hasRecipients = true
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
        SlotSuggestionServiceV2::class,
        HeatMapServiceV2::class,
        SlotSuggestionServiceTestConfiguration::class,
    ]
)
class SlotSuggestionServiceTest {

    @MockBean
    lateinit var meetingDataService: MeetingDataService

    @MockBean
    lateinit var userRepository: UserRepository

    @MockBean
    lateinit var blockRepository: BlockRepository

    @Autowired
    lateinit var config: HeatMapConfiguration

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var underTest: SlotSuggestionServiceV2

    @Test
    fun `non calendar user gets two suggestions today and one next week`() {
        setupPersona(nonCalendarUser)

        val suggestions =
            underTest.suggestSlots(userId, SlotSuggestionRequest(Duration.ofHours(1), ZoneId.of("UTC"), 3))

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
            underTest.suggestSlots(userId, SlotSuggestionRequest(Duration.ofHours(1), ZoneId.of("UTC"), 3))

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

        given(blockRepository.findAllBlocksCached(userId, now.minusDays(config.historicalDays), now))
            .willReturn(persona.pastBlocks())

        given(blockRepository.findAllBlocks(userId, now, now.plusDays(config.futureDays), false))
            .willReturn(persona.futureBlocks())

    }
}
