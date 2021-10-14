package com.sama.api.users

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.users.application.DayWorkingHoursDTO
import com.sama.users.application.GrantUserPermissionsCommand
import com.sama.users.application.MarketingPreferencesDTO
import com.sama.users.application.RevokeUserPermissionsCommand
import com.sama.users.application.UpdateMarketingPreferencesCommand
import com.sama.users.application.UpdateTimeZoneCommand
import com.sama.users.application.UpdateWorkingHoursCommand
import com.sama.users.application.UserSettingsApplicationService
import com.sama.users.application.UserSettingsDTO
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPermission
import com.sama.users.domain.UserPermission.PAST_EVENT_CONTACT_SCAN
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
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
        UserSettingsController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class UserSettingsControllerTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockBean
    lateinit var userSettingsApplicationService: UserSettingsApplicationService

    private val userId = UserId(1)
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    @Test
    fun `get settings`() {
        whenever(userSettingsApplicationService.find(userId))
            .thenReturn(
                UserSettingsDTO(
                    Locale.ENGLISH,
                    ZoneId.of("Europe/Rome"),
                    true,
                    listOf(
                        DayWorkingHoursDTO(MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        DayWorkingHoursDTO(WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(17, 30))
                    ),
                    setOf(PAST_EVENT_CONTACT_SCAN),
                    MarketingPreferencesDTO(true)
                )
            )

        val expectedJson = """
        {
            "locale": "en",
            "timeZone": "Europe/Rome",
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
            ],
            "grantedPermissions": [ "PAST_EVENT_CONTACT_SCAN" ],
            "marketingPreferences": {
                "newsletterSubscriptionEnabled": true
            }
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
        whenever(
            userSettingsApplicationService.updateWorkingHours(
                userId,
                UpdateWorkingHoursCommand(
                    listOf(DayWorkingHoursDTO(TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)))
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

    @Test
    fun `update time zone`() {
        val timeZone = "Europe/Rome"
        whenever(userSettingsApplicationService.updateTimeZone(userId, UpdateTimeZoneCommand(ZoneId.of(timeZone))))
            .thenReturn(true)

        val requestBody = """
            {
                "timeZone": "$timeZone"
            }
        """
        mockMvc.perform(
            post("/api/user/me/update-time-zone")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `grant permissions`() {
        whenever(userSettingsApplicationService.grantPermissions(userId,
            GrantUserPermissionsCommand(setOf(PAST_EVENT_CONTACT_SCAN))))
            .thenReturn(true)

        val requestBody = """
            {
                "permissions": [ "PAST_EVENT_CONTACT_SCAN" ]
            }
        """
        mockMvc.perform(
            post("/api/user/me/grant-permissions")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `revoke permissions`() {
        whenever(userSettingsApplicationService.revokePermissions(userId,
            RevokeUserPermissionsCommand(setOf(PAST_EVENT_CONTACT_SCAN))))
            .thenReturn(true)

        val requestBody = """
            {
                "permissions": [ "PAST_EVENT_CONTACT_SCAN" ]
            }
        """
        mockMvc.perform(
            post("/api/user/me/revoke-permissions")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `update marketing preferences`() {
        whenever(userSettingsApplicationService.updateMarketingPreferences(userId, UpdateMarketingPreferencesCommand(true)))
            .thenReturn(true)

        val requestBody = """
            {
                "newsletterSubscriptionEnabled": "true"
            }
        """
        mockMvc.perform(
            post("/api/user/me/update-marketing-preferences")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        post("/api/user/me/update-working-hours") to HttpStatus.UNAUTHORIZED,
        post("/api/user/me/update-time-zone") to HttpStatus.UNAUTHORIZED,
        post("/api/user/me/grant-permissions") to HttpStatus.UNAUTHORIZED,
        post("/api/user/me/revoke-permissions") to HttpStatus.UNAUTHORIZED,
        post("/api/user/me/update-marketing-preferences") to HttpStatus.UNAUTHORIZED,
        get("/api/user/me/settings") to HttpStatus.UNAUTHORIZED,
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            DynamicTest.dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}