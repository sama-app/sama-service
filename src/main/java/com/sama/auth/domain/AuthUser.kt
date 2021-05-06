package com.sama.auth.domain

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Clock
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
@Table(schema = "auth", name = "user")
@SecondaryTables(
    value = [
        SecondaryTable(
            schema = "auth", name = "user_google_credential",
            pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_id")]
        ),
        SecondaryTable(
            schema = "auth", name = "user_firebase_credential",
            pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_id")]
        )
    ]
)

class AuthUser(email: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(nullable = false)
    private var email: String? = email

    @Embedded
    private var googleCredential: GoogleCredential? = null

    @Embedded
    private var firebaseCredential: FirebaseCredential? = null

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
    fun removeGoogleCredential() {
        this.googleCredential = null
        this.updatedAt = Instant.now()
    }

    /**
     * Refresh Firebase credentials with a new token
     */
    fun registerFirebaseDevice(deviceId: UUID, firebaseRegistrationToken: String) {
        this.firebaseCredential = FirebaseCredential(deviceId, firebaseRegistrationToken, Instant.now())
    }

    fun unregisterFirebaseDevice(deviceId: UUID) {
        this.firebaseCredential = null;
    }

    fun sendPushNotification(message: String, firebaseMessaging: FirebaseMessaging): String {
        if (this.firebaseCredential == null) {
            return "DEVICE NOT REGISTERED"
        }

        val pushNotification = Message.builder()
            .setToken(this.firebaseCredential?.registrationToken)
            .putData("msg", message)
            .build()
        try {
            return firebaseMessaging.send(pushNotification)
        } catch (e: FirebaseMessagingException) {
            return e.message!!
        }
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