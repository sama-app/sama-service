package com.sama.auth.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Clock
import java.util.*

interface JwtConfiguration {
    val signingSecret: String
    val expirationSec: Long
    val keyId: String
}

data class JwtPair(val accessToken: String, val refreshToken: String)

class Jwt {
    val token: String

    @Throws(JWTVerificationException::class)
    constructor(token: String, jwtConfiguration: JwtConfiguration) {
        this.token = token
        verify(jwtConfiguration)
    }

    constructor(user: AuthUser, jwtConfiguration: JwtConfiguration, clock: Clock) {
        this.token = JWT.create()
            .withKeyId(jwtConfiguration.keyId)
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(user.email())
            .withIssuedAt(Date.from(clock.instant()))
            .withExpiresAt(Date.from(clock.instant().plusSeconds(jwtConfiguration.expirationSec)))
            .sign(Algorithm.HMAC256(jwtConfiguration.signingSecret))
    }

    fun decoded(): DecodedJWT {
        return JWT.decode(token)
    }

    private fun verify(jwtConfiguration: JwtConfiguration) {
        val algorithm = Algorithm.HMAC256(jwtConfiguration.signingSecret)
        val jwtVerifier = JWT.require(algorithm).build()
        jwtVerifier.verify(token)
    }
}