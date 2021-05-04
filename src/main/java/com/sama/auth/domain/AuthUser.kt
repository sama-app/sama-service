package com.sama.auth.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Clock
import java.time.Instant
import javax.persistence.*

@Entity
@Table(schema = "auth", name = "user")
@SecondaryTable(
    schema = "auth", name = "user_google_credential",
    pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_id")]
)
class AuthUser(email: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(nullable = false)
    private var email: String? = email

    @Embedded
    private var googleCredential: GoogleCredential? = null

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null

    init {
        this.createdAt = Instant.now()
        this.updatedAt = Instant.now()
    }

    fun id(): Long? {
        return id
    }

    fun email(): String {
        return email!!
    }

    fun googleCredential(): GoogleCredential? {
        return googleCredential
    }

    /**
     * Initialise Google credentials with a new access & refresh token pair
     */
    fun initGoogleCredential(accessToken: String, refreshToken: String, expirationTimeMs: Long) {
        this.googleCredential = GoogleCredential(accessToken, refreshToken, expirationTimeMs, Instant.now())
        this.updatedAt = Instant.now()
    }

    /**
     * Refresh Google credentials with a new access token
     */
    fun refreshGoogleCredential(accessToken: String, expirationTimeMs: Long) {
        check(this.googleCredential?.refreshToken != null) { "cannot refresh google credential" }
        this.googleCredential = this.googleCredential
            ?.copy(accessToken = accessToken, expirationTimeMs = expirationTimeMs, updatedAt = Instant.now())
        this.updatedAt = Instant.now()
    }

    /**
     * Remove Google OAuth2 Credentials, revoking access to Google APIs
     */
    fun removeGoogleCredential(): AuthUser {
        this.googleCredential = null
        this.updatedAt = Instant.now()
        return this
    }

    fun issueJwtPair(
        accessJwtConfiguration: JwtConfiguration,
        refreshJwtConfiguration: JwtConfiguration,
        clock: Clock,
    ): JwtPair {
        val accessToken = Jwt(email!!, accessJwtConfiguration, clock).token
        val refreshToken = Jwt(email!!, refreshJwtConfiguration, clock).token
        return JwtPair(accessToken, refreshToken)
    }

    fun refreshJwt(
        refreshToken: String,
        accessJwtConfiguration: JwtConfiguration,
        refreshJwtConfiguration: JwtConfiguration,
        clock: Clock,
    ): JwtPair {
        val verifiedRefreshToken = Jwt(refreshToken, refreshJwtConfiguration).token
        val accessToken = Jwt(email!!, accessJwtConfiguration, clock).token
        return JwtPair(accessToken, verifiedRefreshToken)
    }
}