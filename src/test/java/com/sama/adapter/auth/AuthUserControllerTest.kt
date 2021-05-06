package com.sama.adapter.auth

import com.sama.adapter.AdapterTestConfiguration
import com.sama.auth.application.AuthUserApplicationService
import com.sama.auth.application.RefreshTokenCommand
import com.sama.auth.application.RegisterDeviceCommand
import com.sama.auth.application.UnregisterDeviceCommand
import com.sama.auth.domain.JwtPair
import com.sama.configuration.WebMvcConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        AuthUserController::class,
        WebMvcConfiguration::class,
        AdapterTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class AuthUserControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var authUserApplicationService: AuthUserApplicationService

    private val jwt = "eyJraWQiOiJkdW1teS1hY2Nlc3Mta2V5LWlkLWZvci1kZXZlbG9wbWVudCIsInR5cCI6IkpXVCIsImFsZyI6IkhTMjU2" +
            "In0.eyJzdWIiOiJiYWx5cytzYW1hQHZhbGVudHVrZXZpY2l1cy5jb20iLCJleHAiOjE2MjI5MTM4NjQsImlhdCI6MTYyMDMyMTg2NC" +
            "wianRpIjoiYTk5MDNiOTEtNjc1ZC00NDExLTg3YjQtZjFhMTk3Y2FjZjdhIn0.kO4SeU-4OO61U0UfkQsAnZW0l1ntjhHy7_k6JhRY" +
            "zg8"

    @Test
    fun `register device`() {
        val userId: Long = 1
        val deviceId = UUID.fromString("075f7e8a-e01c-4f2f-9c3b-ce5d412e618c")
        val registrationToken = "some-token"
        whenever(
            (authUserApplicationService.registerDevice(
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
            post("/api/auth/user/register-device")
                .contentType(MediaType.APPLICATION_JSON)
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
            (authUserApplicationService.unregisterDevice(
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
            post("/api/auth/user/unregister-device")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }


    @Test
    fun `refresh token`() {
        val userId: Long = 1
        val refreshToken = "refresh-token"
        whenever(
            (authUserApplicationService.refreshToken(
                eq(userId),
                eq(RefreshTokenCommand(refreshToken))
            ))
        )
            .thenReturn(JwtPair("access-token", "refresh-token"))

        val requestBody = """
            {
                "refreshToken": "refresh-token"
            }
        """
        val expected = """
            {
                "accessToken": "access-token",
                "refreshToken": "refresh-token"
            }
        """
        mockMvc.perform(
            post("/api/auth/user/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }
}