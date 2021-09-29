package com.sama.api.auth

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleSignErrorDTO
import com.sama.auth.application.GoogleSignSuccessDTO
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.users.application.GoogleOauth2Redirect
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
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

    private val mobileUserAgent = "" +
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) " +
            "AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 " +
            "Mobile/14E5239e Safari/602.1"

    @Test
    fun `google sign in successful`() {
        val accessToken = "access-jwt"
        val refreshToken = "refresh-jwt"

        whenever((googleOauth2ApplicationService.googleSignIn(any())))
            .thenReturn(GoogleSignSuccessDTO(accessToken, refreshToken))

        val requestBody = """
            {
                "authCode": "some-auth-code"
            }
        """.trimIndent()

        val expected = """
           {
                "accessToken": $accessToken,
                "refreshToken": $refreshToken
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/google-sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }

    @Test
    fun `google sign in without required scopes`() {
        whenever((googleOauth2ApplicationService.googleSignIn(any())))
            .thenThrow(GoogleInsufficientPermissionsException(RuntimeException("")))

        val requestBody = """
            {
                "authCode": "some-auth-code"
            }
        """.trimIndent()

        val expected = """
           {
                "reason": "google_insufficient_permissions"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/auth/google-sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(content().json(expected, false))
    }

    @Test
    fun `google authorize returns the authorization url`() {
        val redirectUri = "https://accounts.google.com/o/oauth2/auth?access_type=offline"
        whenever(googleOauth2ApplicationService.generateAuthorizationUrl(any()))
            .thenReturn(GoogleOauth2Redirect(redirectUri))

        val expectedJson = """
        {
            "authorizationUrl": "${redirectUri}"
        }
        """

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/google-authorize"))
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

        whenever((googleOauth2ApplicationService.processGoogleWebOauth2(any(), eq(code), isNull(), isNull())))
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

        whenever((googleOauth2ApplicationService.processGoogleWebOauth2(any(), eq(code), isNull(), isNull())))
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
        whenever((googleOauth2ApplicationService.processGoogleWebOauth2(any(), isNull(), eq(code), isNull())))
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
        whenever((googleOauth2ApplicationService.processGoogleWebOauth2(any(), isNull(), eq(code), isNull())))
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