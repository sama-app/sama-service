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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.util.*
import kotlin.test.assertEquals

class UsersTest {

    private val fixedClock = Clock.fixed(LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(UTC), ZoneId.systemDefault())
    private val jwtId = UUID.fromString("3e5a7567-ffbd-471b-ba26-56b309801fee")
    private val jwtConfiguration = AccessJwtConfiguration("secret", 60, "key-id")

    @Test
    fun `user registration with Google credentials`() {
        val credential = GoogleCredential("access", "refresh", 0)
        UserRegistration(1L, UUID.randomUUID(), "balys@meetsama.com", false, "Balys Val", credential)
    }

    @Test
    fun `user registration with invalid email throws`() {
        assertThrows(InvalidEmailException::class.java) {
            val credential = GoogleCredential("access", "refresh", 0)
            UserRegistration(1L,  UUID.randomUUID(), "invalid-email.com", false, "Balys Val", credential)
        }
    }

    @Test
    fun `user registration with existing email throws`() {
        assertThrows(UserAlreadyExistsException::class.java) {
            val credential = GoogleCredential("access", "refresh", 0)
            UserRegistration(1L,  UUID.randomUUID(), "balys@meetsama.com", true, "Balys Val", credential)
        }
    }

    @Test
    fun `issue jwt`() {
        val userJwtIssuer = UserJwtIssuer(
            1L,
            UUID.fromString("65b977ea-8980-4b1a-a6ee-f8fcc3f1fc24"),
            "balys@yoursama.com",
            true
        )

        val actual = userJwtIssuer.issue(jwtId, jwtConfiguration, fixedClock)
            .assertDoesNotThrowOrNull()

        assertEquals(
            Jwt(
                "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
                        "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjV" +
                        "iOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0IiwiZXhwIj" +
                        "oxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3N" +
                        "TY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
                        "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"
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
        val userJwtIssuer = UserJwtIssuer(1L, UUID.randomUUID(), "balys@meetsama.com", false)

        val actual = userJwtIssuer.issue(jwtId, jwtConfiguration, fixedClock)
        actual.assertThrows(InactiveUserException::class.java)
    }
}