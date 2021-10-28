package com.sama.api.users

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.users.application.RegisterDeviceCommand
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.application.UserDeviceRegistrationService
import com.sama.users.application.UserPublicDTO
import com.sama.users.domain.UserPublicId
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
        UserController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class UserControllerTest(@Autowired val mockMvc: MockMvc) {
    @MockBean
    lateinit var userApplicationService: UserApplicationService

    @MockBean
    lateinit var userDeviceRegistrationService: UserDeviceRegistrationService

    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    @Test
    fun `get user public details`() {
        val userPublicDTO = UserPublicDTO(
            UserPublicId.random(),
            "test name",
            "test@meetsama.com"
        )
        whenever(userApplicationService.me())
            .thenReturn(userPublicDTO)

        val expectedJson = """
        {
           "userId": "${userPublicDTO.userId.id}",
           "fullName": "${userPublicDTO.fullName}",
           "email": "${userPublicDTO.email}"
        }
        """

        mockMvc.perform(
            get("/api/user/me/")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson))
    }

    @Test
    fun `register device`() {
        val deviceId = UUID.fromString("075f7e8a-e01c-4f2f-9c3b-ce5d412e618c")
        val registrationToken = "some-token"
        whenever(
            userDeviceRegistrationService
                .register(RegisterDeviceCommand(deviceId, registrationToken))
        ).thenReturn(true)

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
        val deviceId = UUID.fromString("075f7e8a-e01c-4f2f-9c3b-ce5d412e618c")
        whenever(
            userDeviceRegistrationService.unregister(
                UnregisterDeviceCommand(deviceId)
            )
        ).thenReturn(true)

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

    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        get("/api/user/me/") to UNAUTHORIZED,
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