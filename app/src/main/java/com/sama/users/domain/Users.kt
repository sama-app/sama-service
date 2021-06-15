package com.sama.users.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sama.common.DomainEntity
import com.sama.common.DomainService
import com.sama.common.Factory
import java.time.Clock
import java.util.*
import kotlin.Result.Companion.success

@DomainEntity
data class UserRegistration(val userId: UserId, val email: String, val emailExists: Boolean, val credential: GoogleCredential) {
    init {
        if ('@' !in email) { // todo proper
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
                user.id()!!,
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

@DomainService
data class UserJwtIssuer(val userId: UserId, val email: String, val active: Boolean) {

    @Factory
    companion object {
        fun of(user: UserEntity): UserJwtIssuer {
            return UserJwtIssuer(
                user.id()!!,
                user.email(),
                user.active!!
            )
        }
    }

    fun issue(jwtId: UUID, jwtConfiguration: JwtConfiguration, clock: Clock): Result<Jwt> {
        if (!active) {
            return Result.failure(InactiveUserException())
        }

        return kotlin.runCatching {
            val accessToken = JWT.create()
                .withKeyId(jwtConfiguration.keyId)
                .withJWTId(jwtId.toString())
                .withSubject(email)
                .withIssuedAt(Date.from(clock.instant()))
                .withExpiresAt(Date.from(clock.instant().plusSeconds(jwtConfiguration.expirationSec)))
                .sign(Algorithm.HMAC256(jwtConfiguration.signingSecret))
            Jwt.raw(accessToken).getOrThrow()
        }
    }
}
