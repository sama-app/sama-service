package com.sama.api.auth

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.users.application.JwtPairDTO
import com.sama.users.application.RefreshTokenCommand
import com.sama.users.application.UserApplicationService
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

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        TokenController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class TokenControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var userApplicationService: UserApplicationService

    @Test
    fun `refresh token`() {
        val refreshToken = "refresh-token"
        whenever(userApplicationService.refreshToken(eq(RefreshTokenCommand(refreshToken))))
            .thenReturn(JwtPairDTO("access-token", "refresh-token"))

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
            post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }
}