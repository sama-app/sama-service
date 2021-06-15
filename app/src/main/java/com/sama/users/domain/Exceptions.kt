package com.sama.users.domain

import com.sama.common.DomainValidationException

class InactiveUserException : RuntimeException()
class InvalidEmailException(email: String) : DomainValidationException("Invalid email: '$email'")
class UserAlreadyExistsException(email: String) : RuntimeException("User with email '$email' already exists")