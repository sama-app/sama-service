package com.sama.users.domain

import com.sama.common.DomainEntity
import java.util.UUID
import kotlin.Result.Companion.success
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

@DomainEntity
data class UserDeviceRegistrations(val userId: UserId, val deviceId: UUID?, val firebaseRegistrationToken: String?) {
    fun register(deviceId: UUID, firebaseRegistrationToken: String): Result<UserDeviceRegistrations> {
        return success(
            copy(
                deviceId = deviceId,
                firebaseRegistrationToken = firebaseRegistrationToken
            )
        )
    }

    fun unregister(deviceId: UUID): Result<UserDeviceRegistrations> {
        return success(
            copy(deviceId = null, firebaseRegistrationToken = null)
        )
    }
}