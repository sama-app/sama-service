package com.sama.users.domain

import com.sama.common.DomainValidationException
import com.sama.common.HasReason

class InactiveUserException : RuntimeException()
class InvalidRefreshTokenException : DomainValidationException("Invalid or expired refresh token"), HasReason {
    override val reason = "invalid_refresh_token"
}

class InvalidEmailException(email: String) : DomainValidationException("Invalid email: '$email'")
class UserAlreadyExistsException(email: String) : RuntimeException("User with email '$email' already exists")