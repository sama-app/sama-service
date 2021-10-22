package com.sama.api.calendar

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.calendar.application.CalendarService
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.calendar.application.AddSelectedCalendarCommand
import com.sama.integration.google.calendar.application.CalendarDTO
import com.sama.integration.google.calendar.application.CalendarsDTO
import com.sama.integration.google.calendar.application.RemoveSelectedCalendarCommand
import com.sama.users.domain.UserId
import java.util.UUID
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        CalendarController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class CalendarControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var calendarService: CalendarService

    private val userId = UserId(1)
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    @Test
    fun `fetch calendars`() {
        val accountId = GoogleAccountPublicId(UUID.randomUUID())
        whenever(calendarService.findAll(userId)).thenReturn(
            CalendarsDTO(
                listOf(
                    CalendarDTO(
                        accountId,
                        "primary",
                        true,
                        "title",
                        "#FFFFFF"
                    )
                )
            )
        )

        val expectedJson = """
        {
            "calendars": [
                {
                    "accountId": "${accountId.id}",
                    "calendarId": "primary",
                    "selected": true,
                    "title": "title",
                    "colour": "#FFFFFF"
                }
            ]
        }
        """

        mockMvc.perform(
            get("/api/calendar/calendars")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `fetch calendars empty`() {
        whenever(calendarService.findAll(userId)).thenReturn(CalendarsDTO(emptyList()))

        val expectedJson = """
        {
            "calendars": []
        }
        """

        mockMvc.perform(
            get("/api/calendar/calendars")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `add selected calendar`() {
        val accountId = GoogleAccountPublicId(UUID.randomUUID())
        val calendarId = "primary"
        whenever(calendarService.addSelectedCalendar(userId, AddSelectedCalendarCommand(accountId, calendarId)))
            .thenReturn(true)

        val body = """
        {
            "accountId": "${accountId.id}",
            "calendarId": "primary"
        }
        """

        mockMvc.perform(
            post("/api/calendar/calendars/add")
                .content(body)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `remove selected calendar`() {
        val accountId = GoogleAccountPublicId(UUID.randomUUID())
        val calendarId = "primary"
        whenever(calendarService.removeSelectedCalendar(userId, RemoveSelectedCalendarCommand(accountId, calendarId)))
            .thenReturn(true)

        val body = """
        {
            "accountId": "${accountId.id}",
            "calendarId": "primary"
        }
        """

        mockMvc.perform(
            post("/api/calendar/calendars/remove")
                .content(body)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        get("/api/calendar/calendars/add") to UNAUTHORIZED,
        post("/api/calendar/calendars/remove") to UNAUTHORIZED,
        get("/api/calendar/calendars") to UNAUTHORIZED,
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            DynamicTest.dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}