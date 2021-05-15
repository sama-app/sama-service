package com.sama.api.auth

import com.sama.api.ApiTestConfiguration
import com.sama.users.application.GoogleOauth2ApplicationService
import com.sama.users.application.GoogleOauth2Failure
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.application.GoogleOauth2Success
import com.sama.api.config.WebMvcConfiguration
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
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
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

    private val mobileUserAgent = "" +
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) " +
            "AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 " +
            "Mobile/14E5239e Safari/602.1"

    @Test
    fun `google authorize returns the authorization url`() {
        val redirectUri = "https://accounts.google.com/o/oauth2/auth?access_type=offline"
        whenever(googleOauth2ApplicationService.beginGoogleOauth2(any()))
            .thenReturn(GoogleOauth2Redirect(redirectUri))

        val expectedJson = """
        {
            "authorizationUrl": "${redirectUri}"
        }
        """

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/google-authorize"))
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(expectedJson, true)
            )
    }

    @Test
    fun `google oauth2 callback success for mobile`() {
        val code = "google-success-code"
        val accessToken = "access-jwt"
        val refreshToken = "refresh-jwt"

        whenever((googleOauth2ApplicationService.processGoogleOauth2(any(), eq(code), isNull())))
            .thenReturn(GoogleOauth2Success(accessToken, refreshToken))

        mockMvc.perform(
            get("/api/auth/google-oauth2")
                .queryParam("code", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "yoursama://auth/success" +
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

        whenever((googleOauth2ApplicationService.processGoogleOauth2(any(), eq(code), isNull())))
            .thenReturn(GoogleOauth2Success(accessToken, refreshToken))

        mockMvc.perform(get("/api/auth/google-oauth2").queryParam("code", code))
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "http://localhost/api/auth/success" +
                            "?accessToken=${accessToken}" +
                            "&refreshToken=${refreshToken}"
                )
            )
    }

    @Test
    fun `google oauth2 callback error for mobile`() {
        val code = "google-error-code"
        val error = "error-message"
        whenever((googleOauth2ApplicationService.processGoogleOauth2(any(), isNull(), eq(code))))
            .thenReturn(GoogleOauth2Failure(error))

        mockMvc.perform(
            get("/api/auth/google-oauth2")
                .queryParam("error", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "yoursama://auth/error?error=${error}"
                )
            )
    }


    @Test
    fun `google oauth2 callback error for desktop`() {
        val code = "google-error-code"
        val error = "error_message"
        whenever((googleOauth2ApplicationService.processGoogleOauth2(any(), isNull(), eq(code))))
            .thenReturn(GoogleOauth2Failure(error))

        mockMvc.perform(get("/api/auth/google-oauth2").queryParam("error", code))
            .andExpect(status().isFound)
            .andExpect(
                redirectedUrl(
                    "http://localhost/api/auth/error?error=${error}"
                )
            )
    }
}