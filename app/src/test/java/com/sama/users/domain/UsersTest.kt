package com.sama.users.domain

import com.auth0.jwt.JWT
import com.sama.api.jwtKeyId
import com.sama.common.assertDoesNotThrowOrNull
import com.sama.common.assertThrows
import com.sama.users.configuration.AccessJwtConfiguration
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant.ofEpochSecond
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals

class UsersTest {

    private val fixedClock = Clock.fixed(ofEpochSecond(3600), ZoneId.systemDefault())
    private val jwtId = UUID.fromString("3e5a7567-ffbd-471b-ba26-56b309801fee")
    private val jwtConfiguration = AccessJwtConfiguration("secret", 60, "key-id")

    @Test
    fun `user registration with Google credentials`() {
        val credential = GoogleCredential("access", "refresh", 0)
        UserRegistration(1L, "balys@meetsama.com", false, credential)
    }

    @Test
    fun `user registration with invalid email throws`() {
        assertThrows(InvalidEmailException::class.java) {
            val credential = GoogleCredential("access", "refresh", 0)
            UserRegistration(1L, "invalid-email.com", false, credential)
        }
    }

    @Test
    fun `user registration with existing email throws`() {
        assertThrows(UserAlreadyExistsException::class.java) {
            val credential = GoogleCredential("access", "refresh", 0)
            UserRegistration(1L, "balys@meetsama.com", true, credential)
        }
    }

    @Test
    fun `issue jwt`() {
        val userJwtIssuer = UserJwtIssuer(1L, "balys@yoursama.com", true)

        val actual = userJwtIssuer.issue(jwtId, jwtConfiguration, fixedClock)
            .assertDoesNotThrowOrNull()

        assertEquals(
            Jwt(
                "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
                        "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJleHAiOjM2NjAsImlhdCI6" +
                        "MzYwMCwianRpIjoiM2U1YTc1NjctZmZiZC00NzFiLWJhMjYtNTZiMzA5ODAxZmVlIn0." +
                        "LMJxSJ_AocVwEPiS_yTuhdvyts_hmFOoV9zj4m8Zzgk"
            ),
            actual
        )

        val decodedJwt = JWT.decode(actual.token)
        assertEquals("balys@yoursama.com", decodedJwt.subject)
        assertEquals("key-id", decodedJwt.keyId)
        assertEquals(jwtId.toString(), decodedJwt.id)
    }

    @Test
    fun `issue jwt for inactive user throws`() {
        val userJwtIssuer = UserJwtIssuer(1L, "balys@meetsama.com", false)

        val actual = userJwtIssuer.issue(jwtId, jwtConfiguration, fixedClock)
        actual.assertThrows(InactiveUserException::class.java)
    }
}