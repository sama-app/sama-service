package com.sama.users.domain

import com.auth0.jwt.JWT
import com.sama.common.assertDoesNotThrowOrNull
import com.sama.common.assertThrows
import com.sama.users.configuration.AccessJwtConfiguration
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UsersTest {

    private val fixedClock = Clock.fixed(LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(UTC), ZoneId.systemDefault())
    private val jwtId = UUID.fromString("3e5a7567-ffbd-471b-ba26-56b309801fee")
    private val jwtConfiguration = AccessJwtConfiguration("secret", 60, "key-id")

    @Test
    fun `valid user registration`() {
        UserRegistration("balys@meetsama.com", false, "Balys Val")
            .validate()
    }

    @Test
    fun `user registration with invalid email throws`() {
        assertThrows(InvalidEmailException::class.java) {
            UserRegistration("invalid-email.com", false, "Balys Val")
                .validate()
        }
    }

    @Test
    fun `user registration with existing email throws`() {
        assertThrows(UserAlreadyExistsException::class.java) {
            UserRegistration("balys@meetsama.com", true, "Balys Val")
                .validate()
        }
    }

    @Test
    fun `issue jwt`() {
        val userJwtIssuer = UserJwtIssuer(
            UserDetails(
                UserId(1),
                UserPublicId.of("65b977ea-8980-4b1a-a6ee-f8fcc3f1fc24"),
                "balys@yoursama.com",
                "balys",
                true
            )
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
        val userJwtIssuer = UserJwtIssuer(
            UserDetails(
                UserId(1),
                UserPublicId.of("65b977ea-8980-4b1a-a6ee-f8fcc3f1fc24"),
                "balys@yoursama.com",
                "balys",
                false
            )
        )

        val actual = userJwtIssuer.issue(jwtId, jwtConfiguration, fixedClock)
        actual.assertThrows(InactiveUserException::class.java)
    }
}