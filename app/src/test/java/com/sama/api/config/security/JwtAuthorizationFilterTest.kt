package com.sama.api.config.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.api.jwtKeyId
import com.sama.api.jwtSigningSecret
import com.sama.users.domain.USER_ID_CLAIM
import java.time.Clock
import java.util.Date
import java.util.UUID
import javax.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        WebMvcConfiguration::class,
        ApiTestConfiguration::class,
        AuthTestController::class
    ]
)
@AutoConfigureMockMvc
internal class AuthorizationFiltersTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val clock: Clock
) {

    private val validJwt = JWT.create()
        .withKeyId(jwtKeyId)
        .withSubject("hermione@granger.hg")
        .withClaim(USER_ID_CLAIM, UUID.randomUUID().toString())
        .withIssuedAt(Date.from(clock.instant()))
        .withExpiresAt(Date.from(clock.instant().plusSeconds(3600)))
        .sign(Algorithm.HMAC256(jwtSigningSecret))

    private val expiredJwt = JWT.create()
        .withKeyId(jwtKeyId)
        .withSubject("hermione@granger.hg")
        .withClaim(USER_ID_CLAIM, UUID.randomUUID().toString())
        .withIssuedAt(Date.from(clock.instant()))
        .withExpiresAt(Date.from(clock.instant().plusSeconds(-1)))
        .sign(Algorithm.HMAC256(jwtSigningSecret))

    @Test
    fun `jwt authorization works`() {
        mockMvc.perform(
            get("/api/test-endpoint")
                .header("Authorization", "Bearer $validJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `missing access token returns 401`() {
        mockMvc.perform(get("/api/test-endpoint"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `authorization with invalid header returns 401`() {
        mockMvc.perform(
            get("/api/test-endpoint")
                .header("Authorization", "Invalid $validJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `authorization with expired jwt returns 401`() {
        mockMvc.perform(
            get("/api/test-endpoint")
                .header("Authorization", "Bearer $expiredJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `authorization with cookie jwt works`() {
        mockMvc.perform(
            get("/api/test-endpoint")
                .cookie(Cookie("sama.access", validJwt))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `authorization with expired cookie jwt returns 401`() {
        mockMvc.perform(
            get("/api/test-endpoint")
                .cookie(Cookie("sama.access", expiredJwt))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `no jwt for no-auth endpoint returns 200`() {
        mockMvc.perform(
            get("/api/auth/no-auth")
                .header("Authorization", "Bearer $validJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `invalid auth header for no-auth endpoint returns 200`() {
        mockMvc.perform(
            get("/api/auth/no-auth")
                .header("Authorization", "Bearer GIBBERISH")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `expired jwt for no-auth endpoint returns 401`() {
        mockMvc.perform(
            get("/api/auth/no-auth")
                .header("Authorization", "Bearer $expiredJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }
}

@RestController
class AuthTestController {

    @GetMapping("/api/test-endpoint")
    fun testEndpoint() = "alohomora!"


    @GetMapping("/api/auth/no-auth")
    fun unauthenticatedEndpoint() = "alohomora!"
}