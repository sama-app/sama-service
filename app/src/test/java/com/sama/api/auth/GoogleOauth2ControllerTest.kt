package com.sama.api.auth

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleSignErrorDTO
import com.sama.auth.application.GoogleSignSuccessDTO
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.domain.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        GoogleOauth2Controller::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class GoogleOauth2ControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var googleOauth2ApplicationService: GoogleOauth2ApplicationService

    private val userId = UserId(1)
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    private val mobileUserAgent = "" +
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) " +
            "AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 " +
            "Mobile/14E5239e Safari/602.1"

    @Test
    fun `google authorize returns the authorization url`() {
        val redirectUri = "https://accounts.google.com/o/oauth2/auth?access_type=offline"
        whenever(googleOauth2ApplicationService.generateAuthorizationUrl(any(), isNull()))
            .thenReturn(GoogleOauth2Redirect(redirectUri))

        val expectedJson = """
        {
            "authorizationUrl": "${redirectUri}"
        }
        """

        mockMvc.perform(post("/api/auth/google-authorize"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(expectedJson, true)
            )
    }

    @Test
    fun `google oauth2 callback success for mobile`() {
        val code = "google-success-code"
        val accessToken = "access-jwt"
        val refreshToken = "refresh-jwt"

        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), eq(code), isNull(), isNull())))
            .thenReturn(GoogleSignSuccessDTO(accessToken, refreshToken))

        mockMvc.perform(
            get("/api/auth/google-oauth2")
                .queryParam("code", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "meetsama://auth/success" +
                            "?accessToken=${accessToken}" +
                            "&refreshToken=${refreshToken}"
                )
            )
    }


    @Test
    fun `google oauth2 callback success for desktop`() {
        val code = "google-success-code"
        val accessToken = "access-jwt"
        val refreshToken = "refresh-jwt"

        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), eq(code), isNull(), isNull())))
            .thenReturn(GoogleSignSuccessDTO(accessToken, refreshToken))

        mockMvc.perform(get("/api/auth/google-oauth2").queryParam("code", code))
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "meetsama://auth/success" +
                            "?accessToken=${accessToken}" +
                            "&refreshToken=${refreshToken}"
                )
            )
    }

    @Test
    fun `google oauth2 callback error for mobile`() {
        val code = "google-error-code"
        val error = "error-message"
        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), isNull(), eq(code), isNull())))
            .thenReturn(GoogleSignErrorDTO(error))

        mockMvc.perform(
            get("/api/auth/google-oauth2")
                .queryParam("error", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "meetsama://auth/error?reason=${error}"
                )
            )
    }


    @Test
    fun `google oauth2 callback error for desktop`() {
        val code = "google-error-code"
        val error = "error_message"
        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), isNull(), eq(code), isNull())))
            .thenReturn(GoogleSignErrorDTO(error))

        mockMvc.perform(get("/api/auth/google-oauth2").queryParam("error", code))
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "meetsama://auth/error?reason=${error}"
                )
            )
    }
}