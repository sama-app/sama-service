package com.sama.api.integration.google

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.LinkGoogleAccountErrorDTO
import com.sama.auth.application.LinkGoogleAccountSuccessDTO
import com.sama.integration.google.auth.application.GoogleAccountDTO
import com.sama.integration.google.auth.application.GoogleAccountApplicationService
import com.sama.integration.google.auth.application.GoogleIntegrationsDTO
import com.sama.integration.google.auth.application.UnlinkGoogleAccountCommand
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.domain.UserId
import java.util.UUID
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        GoogleIntegrationController::class,
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

    @MockBean
    lateinit var googleAccountService: GoogleAccountApplicationService

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
    fun `list linked google accounts`() {
        val googleAccount = GoogleAccountDTO(GoogleAccountPublicId(UUID.randomUUID()), "test@meetsama.com")
        whenever(googleAccountService.findAllLinked())
            .thenReturn(GoogleIntegrationsDTO(listOf(googleAccount)))

        val expected = """
            {
                "linkedAccounts": [
                    {
                        "id": "${googleAccount.id.id}",
                        "email": "${googleAccount.email}"
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            get("/api/integration/google")
                .header("Authorization", "Bearer $jwt")
        ).andExpect(status().isOk)
            .andExpect(content().json(expected, true))
    }

    @Test
    fun `unlink google account`() {
        val googleAccountId = GoogleAccountPublicId(UUID.randomUUID())
        whenever(googleAccountService.unlinkAccount(UnlinkGoogleAccountCommand(googleAccountId)))
            .thenReturn(true)

        val request = """
        {
            "googleAccountId": "${googleAccountId.id}"
        }
        """

        mockMvc.perform(
            post("/api/integration/google/unlink-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `link google account`() {
        val inputUrl = "http://localhost/api/integration/google/callback"
        val redirectUri = "https://accounts.google.com/o/oauth2/auth?access_type=offline"
        whenever(googleOauth2ApplicationService.generateAuthorizationUrl(inputUrl))
            .thenReturn(GoogleOauth2Redirect(redirectUri))

        val expectedJson = """
        {
            "authorizationUrl": "${redirectUri}"
        }
        """

        mockMvc.perform(
            post("/api/integration/google/link-account")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `link google account without authorization fails`() {
        mockMvc.perform(post("/api/integration/google/link-account"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `google oauth2 callback success for mobile`() {
        val code = "google-success-code"

        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), eq(code), isNull(), isNull())))
            .thenReturn(LinkGoogleAccountSuccessDTO(GoogleAccountPublicId(UUID.randomUUID())))

        mockMvc.perform(
            get("/api/integration/google/callback")
                .queryParam("code", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("meetsama://integration/google/link-account/success"))
    }


    @Test
    fun `google oauth2 callback success for desktop`() {
        val code = "google-success-code"

        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), eq(code), isNull(), isNull())))
            .thenReturn(LinkGoogleAccountSuccessDTO(GoogleAccountPublicId(UUID.randomUUID())))

        mockMvc.perform(get("/api/integration/google/callback").queryParam("code", code))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("meetsama://integration/google/link-account/success"))
    }

    @Test
    fun `google oauth2 callback error for mobile`() {
        val code = "google-error-code"
        val error = "error-message"
        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), isNull(), eq(code), isNull())))
            .thenReturn(LinkGoogleAccountErrorDTO(error))

        mockMvc.perform(
            get("/api/integration/google/callback")
                .queryParam("error", code)
                .header("User-Agent", mobileUserAgent)
        )
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("meetsama://integration/google/link-account/error?reason=${error}"))
    }


    @Test
    fun `google oauth2 callback error for desktop`() {
        val code = "google-error-code"
        val error = "error_message"
        whenever((googleOauth2ApplicationService.processOauth2Callback(any(), isNull(), eq(code), isNull())))
            .thenReturn(LinkGoogleAccountErrorDTO(error))

        mockMvc.perform(get("/api/integration/google/callback").queryParam("error", code))
            .andExpect(status().isFound)
            .andExpect(redirectedUrl("meetsama://integration/google/link-account/error?reason=${error}"))
    }


    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        get("/api/integration/google") to HttpStatus.UNAUTHORIZED,
        post("/api/integration/google/link-account") to HttpStatus.UNAUTHORIZED,
        post("/api/integration/google/unlink-account") to HttpStatus.UNAUTHORIZED,
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            DynamicTest.dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}