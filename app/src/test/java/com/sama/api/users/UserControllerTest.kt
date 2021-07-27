package com.sama.api.users

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.users.application.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo
import java.time.DayOfWeek.*
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        UserController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class UserControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var userApplicationService: UserApplicationService

    private val userId: Long = 1
    private val jwt = "eyJraWQiOiJkdW1teS1hY2Nlc3Mta2V5LWlkLWZvci1kZXZlbG9wbWVudCIsInR5cCI6IkpXVCIsImFsZyI6IkhTMjU2" +
            "In0.eyJzdWIiOiJiYWx5cytzYW1hQHZhbGVudHVrZXZpY2l1cy5jb20iLCJleHAiOjE2MjI5MTM4NjQsImlhdCI6MTYyMDMyMTg2NC" +
            "wianRpIjoiYTk5MDNiOTEtNjc1ZC00NDExLTg3YjQtZjFhMTk3Y2FjZjdhIn0.kO4SeU-4OO61U0UfkQsAnZW0l1ntjhHy7_k6JhRY" +
            "zg8"

    @Test
    fun `delete user`() {
        whenever(userApplicationService.deleteUser(eq(userId))).thenReturn(true)

        mockMvc.perform(
            post("/api/user/me/delete")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `register device`() {
        val deviceId = UUID.fromString("075f7e8a-e01c-4f2f-9c3b-ce5d412e618c")
        val registrationToken = "some-token"
        whenever(
            (userApplicationService.registerDevice(
                eq(userId),
                eq(RegisterDeviceCommand(deviceId, registrationToken))
            ))
        )
            .thenReturn(true)

        val requestBody = """
            {
                "deviceId": "075f7e8a-e01c-4f2f-9c3b-ce5d412e618c",
                "firebaseRegistrationToken": "some-token"
            }
        """
        mockMvc.perform(
            post("/api/user/me/register-device")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `unregister device`() {
        val userId: Long = 1
        val deviceId = UUID.fromString("075f7e8a-e01c-4f2f-9c3b-ce5d412e618c")
        whenever(
            (userApplicationService.unregisterDevice(
                eq(userId),
                eq(UnregisterDeviceCommand(deviceId))
            ))
        )
            .thenReturn(true)

        val requestBody = """
            {
                "deviceId": "075f7e8a-e01c-4f2f-9c3b-ce5d412e618c"
            }
        """
        mockMvc.perform(
            post("/api/user/me/unregister-device")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `get settings`() {
        whenever(userApplicationService.findUserSettings(eq(userId)))
            .thenReturn(
                UserSettingsDTO(
                    Locale.ENGLISH,
                    ZoneId.of("Europe/Rome"),
                    true,
                    listOf(
                        DayWorkingHoursDTO(MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        DayWorkingHoursDTO(WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 30))
                    )
                )
            )

        val expectedJson = """
        {
            "locale": "en",
            "timezone": "Europe/Rome",
            "format24HourTime": true,
            "workingHours": [
                {
                    "dayOfWeek": "MONDAY",
                    "startTime": "10:00:00",
                    "endTime": "12:00:00"
                },
                {
                    "dayOfWeek": "WEDNESDAY",
                    "startTime": "13:00:00",
                    "endTime": "17:30:00"
                }
            ]
        }
        """

        mockMvc.perform(
            get("/api/user/me/settings")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `update working hours`() {
        val userId: Long = 1
        whenever(
            userApplicationService.updateWorkingHours(
                eq(userId),
                eq(
                    UpdateWorkingHoursCommand(
                        listOf(DayWorkingHoursDTO(TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)))
                    )
                )
            )
        )
            .thenReturn(true)

        val requestBody = """
            {
                "workingHours": [
                    {
                        "dayOfWeek": "TUESDAY",
                        "startTime": "10:00:00",
                        "endTime": "12:00:00"
                    }
                ]
            }
        """
        mockMvc.perform(
            post("/api/user/me/update-working-hours")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        post("/api/user/me/update-working-hours") to UNAUTHORIZED,
        get("/api/user/me/settings") to UNAUTHORIZED,
        post("/api/user/me/unregister-device") to UNAUTHORIZED,
        post("/api/user/me/register-device") to UNAUTHORIZED,
        post("/api/user/me/delete") to UNAUTHORIZED
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            DynamicTest.dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}