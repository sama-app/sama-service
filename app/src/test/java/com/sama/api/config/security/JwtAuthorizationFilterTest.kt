package com.sama.api.config.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sama.api.ApiTestConfiguration
import com.sama.api.jwtKeyId
import com.sama.api.jwtSigningSecret
import com.sama.api.config.WebMvcConfiguration
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

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
        .withIssuedAt(Date.from(clock.instant()))
        .withExpiresAt(Date.from(clock.instant().plusSeconds(3600)))
        .sign(Algorithm.HMAC256(jwtSigningSecret))

    private val expiredJwt = JWT.create()
        .withKeyId(jwtKeyId)
        .withSubject("hermione@granger.hg")
        .withIssuedAt(Date.from(clock.instant()))
        .withExpiresAt(Date.from(clock.instant().plusSeconds(-1)))
        .sign(Algorithm.HMAC256(jwtSigningSecret))

    @Test
    fun `jwt authorization works`() {
        mockMvc.perform(
            get("/test-endpoint")
                .header("Authorization", "Bearer $validJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `missing access token returns 403`() {
        mockMvc.perform(get("/test-endpoint"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `authorization with invalid header returns 403`() {
        mockMvc.perform(
            get("/test-endpoint")
                .header("Authorization", "Invalid $validJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `authorization with expired jwt returns 403`() {
        mockMvc.perform(
            get("/test-endpoint")
                .header("Authorization", "Bearer $expiredJwt")
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

}

@RestController
class AuthTestController {

    @GetMapping("test-endpoint")
    fun testEndpoint() = "alohomora!"
}