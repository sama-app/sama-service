package com.sama.users.domain

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UsersTest {

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
}