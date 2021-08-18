package com.sama.users.domain

import com.sama.common.DomainEntity
import org.apache.commons.validator.routines.EmailValidator

@DomainEntity
data class UserDetails(
    val id: UserId? = null,
    val publicId: UserPublicId? = null,
    val email: String,
    val fullName: String?,
    val active: Boolean
) {
    fun rename(fullName: String?): UserDetails {
        return copy(fullName = fullName)
    }
}

@DomainEntity
data class UserRegistration(
    val email: String,
    val emailExists: Boolean,
    val fullName: String?
) {
    fun validate(): UserDetails {
        if (EmailValidator.getInstance().isValid(email).not()) {
            throw InvalidEmailException(email)
        }

        if (emailExists) {
            throw UserAlreadyExistsException(email)
        }

        return UserDetails(email = email, fullName = fullName, active = true)
    }
}

