package com.sama.users.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sama.common.DomainEntity
import com.sama.common.DomainService
import com.sama.common.Factory
import java.time.Clock
import java.util.Date
import java.util.UUID
import kotlin.Result.Companion.success
import org.apache.commons.validator.routines.EmailValidator

@DomainEntity
data class UserRegistration(
    val userId: UserId,
    val publicId: UserPublicId,
    val email: String,
    val emailExists: Boolean,
    val fullName: String?,
    val credential: GoogleCredential
) {
    init {
        if (EmailValidator.getInstance().isValid(email).not()) {
            throw InvalidEmailException(email)
        }

        if (emailExists) {
            throw UserAlreadyExistsException(email)
        }
    }
}

@DomainEntity
data class UserDeviceRegistrations(val userId: UserId, val deviceId: UUID?, val firebaseRegistrationToken: String?) {

    @Factory
    companion object {
        fun of(user: UserEntity): UserDeviceRegistrations {
            return UserDeviceRegistrations(
                user.id,
                user.firebaseCredential?.deviceId,
                user.firebaseCredential?.registrationToken
            )
        }
    }

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

const val USER_ID_CLAIM = "user_id"

@DomainService
data class UserJwtIssuer(val userId: UserId, val userPublicId: UserPublicId, val email: String, val active: Boolean) {

    @Factory
    companion object {
        fun of(user: UserEntity): UserJwtIssuer {
            return UserJwtIssuer(
                user.id,
                user.publicId,
                user.email,
                user.active!!
            )
        }
    }

    fun issue(jwtId: UUID, jwtConfiguration: JwtConfiguration, clock: Clock): Result<Jwt> {
        return kotlin.runCatching {
            if (!active) {
                throw InactiveUserException()
            }

            val accessToken = JWT.create()
                .withKeyId(jwtConfiguration.keyId)
                .withJWTId(jwtId.toString())
                .withSubject(email)
                .withClaim(USER_ID_CLAIM, userPublicId.toString())
                .withIssuedAt(Date.from(clock.instant()))
                .withExpiresAt(Date.from(clock.instant().plusSeconds(jwtConfiguration.expirationSec)))
                .sign(Algorithm.HMAC256(jwtConfiguration.signingSecret))
            Jwt.raw(accessToken).getOrThrow()
        }
    }
}
