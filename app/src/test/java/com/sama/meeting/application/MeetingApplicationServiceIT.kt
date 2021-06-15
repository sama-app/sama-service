package com.sama.meeting.application

import com.sama.meeting.domain.MeetingIntentRepository
import com.sama.common.assertDoesNotThrowOrNull
import com.sama.common.findByIdOrThrow
import com.sama.meeting.domain.MeetingIntent
import com.sama.slotsuggestion.application.SlotSuggestion
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionResponse
import com.sama.slotsuggestion.application.SlotSuggestionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
@Transactional
class MeetingApplicationServiceIT {

    companion object {
        @Container
        val container: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:13-alpine")
            .apply {
                withDatabaseName("sama-test")
                withUsername("test")
                withPassword("password")
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", container::getJdbcUrl)
            registry.add("spring.datasource.username", container::getUsername)
            registry.add("spring.datasource.password", container::getPassword)
        }
    }

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var meetingIntentRepository: MeetingIntentRepository

    @MockBean
    lateinit var slotSuggestionService: SlotSuggestionService

    @Autowired
    lateinit var underTest: MeetingApplicationService


    @Test
    fun testInitiateMeeting() {
        // setup
        val userId = 1L
        val command = InitiateMeetingCommand(30, ZoneId.systemDefault(), 1, 1)

        val slotSuggestion = SlotSuggestion(
            ZonedDateTime.now(clock),
            ZonedDateTime.now(clock).plusMinutes(30),
            1.0
        )
        whenever(slotSuggestionService.suggestSlots(eq(userId), any()))
            .thenReturn(SlotSuggestionResponse(listOf(slotSuggestion)))

        // act
        val meetingIntent = underTest.initiateMeeting(userId, command)

        // verify
        val persisted = meetingIntentRepository.findByIdOrThrow(meetingIntent.meetingIntentId)
        MeetingIntent.of(persisted).assertDoesNotThrowOrNull()

        val expectedSlotSuggestionRequest = SlotSuggestionRequest(
            Duration.ofMinutes(30),
            ZoneId.systemDefault(),
            1,
            LocalDateTime.now(clock),
            LocalDateTime.now(clock).plusDays(1)
        )
        verify(slotSuggestionService).suggestSlots(eq(1), eq(expectedSlotSuggestionRequest))

        val expectedDTO = MeetingIntentDTO(
            1L, 1L, RecipientDTO(null, null), 30,
            listOf(MeetingSlotDTO(ZonedDateTime.now(clock), ZonedDateTime.now(clock).plusMinutes(30)))
        )
        assertEquals(expectedDTO, meetingIntent)
    }

    @TestConfiguration
    class Config {
        @Bean
        fun clock(): Clock {
            val fixedDate = LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            return Clock.fixed(fixedDate, ZoneId.systemDefault());
        }
    }
}