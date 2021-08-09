package com.sama.api.calendar

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.calendar.application.EventApplicationService
import com.sama.calendar.application.EventDTO
import com.sama.calendar.application.FetchEventsDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        EventController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class EventControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var eventApplicationService: EventApplicationService

    private val userId: Long = 1
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    @Test
    fun `fetch blocks with valid dates`() {
        val startDate = LocalDate.of(2021, 1, 1)
        val endDate = LocalDate.of(2021, 1, 2)
        val zoneId = ZoneId.of("Europe/Rome")

        val startDateTime = ZonedDateTime.of(startDate, LocalTime.of(12, 15), zoneId)
        val endDateTime = ZonedDateTime.of(startDate, LocalTime.of(12, 30), zoneId)
        val eventDTO = EventDTO(startDateTime, endDateTime, false, "test")
        whenever(eventApplicationService.fetchEvents(eq(userId), eq(startDate), eq(endDate), eq(zoneId)))
            .thenReturn(FetchEventsDTO(listOf(eventDTO), listOf(eventDTO)))

        val expectedJson = """
        {
            "blocks": [
                {
                    "startDateTime": "2021-01-01T11:15:00Z",
                    "endDateTime": "2021-01-01T11:30:00Z",
                    "allDay": false,
                    "title": "test"
                }
            ],
            "events": [
                {
                    "startDateTime": "2021-01-01T11:15:00Z",
                    "endDateTime": "2021-01-01T11:30:00Z",
                    "allDay": false,
                    "title": "test"
                }
            ]
        }
        """

        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `fetch blocks empty`() {
        val startDate = LocalDate.of(2021, 1, 1)
        val endDate = LocalDate.of(2021, 1, 2)
        val zoneId = ZoneId.of("Europe/Rome")

        whenever(eventApplicationService.fetchEvents(eq(userId), eq(startDate), eq(endDate), eq(zoneId)))
            .thenReturn(FetchEventsDTO(emptyList(), emptyList()))

        val expectedJson = """
        {
            "blocks": [
            ],
            "events": [
            ]
        }
        """

        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `fetch blocks with non-iso dates fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "01/01/2021")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks with endDate before startDate fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-03")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks with invalid timezone fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-03")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Invalid")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks without authorization fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isUnauthorized)
    }
}