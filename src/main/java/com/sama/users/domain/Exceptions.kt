package com.sama.users.domain

class InactiveUserException : RuntimeException()
class InvalidEmailException(email: String) : RuntimeException("Invalid email: '$email'")
class UserAlreadyExistsException(email: String) : RuntimeException("User with email '$email' already exists")