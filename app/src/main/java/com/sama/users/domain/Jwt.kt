package com.sama.users.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.sama.common.DomainService
import com.sama.common.ValueObject
import java.time.Clock
import java.util.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

interface JwtConfiguration {
    val signingSecret: String
    val expirationSec: Long
    val keyId: String
}

const val USER_ID_CLAIM = "user_id"

@DomainService
data class UserJwtIssuer(val userDetails: UserDetails) {

    fun issue(jwtId: UUID, jwtConfiguration: JwtConfiguration, clock: Clock): Result<Jwt> {
        return kotlin.runCatching {
            if (!userDetails.active) {
                throw InactiveUserException()
            }

            val accessToken = JWT.create()
                .withKeyId(jwtConfiguration.keyId)
                .withJWTId(jwtId.toString())
                .withSubject(userDetails.email)
                .withClaim(USER_ID_CLAIM, userDetails.publicId?.id.toString())
                .withIssuedAt(Date.from(clock.instant()))
                .withExpiresAt(Date.from(clock.instant().plusSeconds(jwtConfiguration.expirationSec)))
                .sign(Algorithm.HMAC256(jwtConfiguration.signingSecret))
            Jwt.raw(accessToken).getOrThrow()
        }
    }
}

@ValueObject
data class Jwt(val token: String) {

    companion object {
        fun verified(token: String, jwtConfiguration: JwtConfiguration, clock: Clock): Result<Jwt> {
            val refreshToken = Jwt(token)
            return try {
                refreshToken.verify(jwtConfiguration, clock)
                success(refreshToken)
            } catch (e: JWTVerificationException) {
                failure(e)
            }
        }

        fun raw(token: String): Result<Jwt> {
            return success(Jwt(token))
        }
    }

    private fun verify(jwtConfiguration: JwtConfiguration, clock: Clock) {
        val algorithm = Algorithm.HMAC256(jwtConfiguration.signingSecret)
        val jwtVerifier = (JWT.require(algorithm) as JWTVerifier.BaseVerification)
            .build { Date.from(clock.instant()) }
        jwtVerifier.verify(token)
    }

    private fun decoded(): DecodedJWT {
        return JWT.decode(token)
    }

    fun userId(): UserPublicId? {
        return decoded().getClaim(USER_ID_CLAIM)?.asString()?.let { UserPublicId.of(it) }
    }

    fun userEmail(): String {
        return decoded().subject
    }
}
