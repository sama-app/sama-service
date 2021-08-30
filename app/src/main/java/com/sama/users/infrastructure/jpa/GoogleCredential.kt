package com.sama.users.infrastructure.jpa

import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable
import org.springframework.security.crypto.encrypt.TextEncryptor

/**
 * Object to store Google access & refresh tokens. Fields are marked nullable to catch cases
 * where access token is removed when Sama app permissions are removed.
 */
@Embeddable
data class GoogleCredential(
    @Column(name = "google_access_token_encrypted", table = "user_google_credential")
    val accessTokenEncrypted: String? = null,

    @Column(name = "google_refresh_token_encrypted", table = "user_google_credential")
    val refreshTokenEncrypted: String? = null,

    @Column(name = "google_token_expiration_time_ms", table = "user_google_credential")
    val expirationTimeMs: Long?,

    @Column(name = "updated_at", table = "user_google_credential")
    var updatedAt: Instant? = null,
) {
    companion object {
        fun encrypted(
            accessToken: String?,
            refreshToken: String?,
            expirationTimeMs: Long?,
            encryptor: TextEncryptor,
        ): GoogleCredential {
            return GoogleCredential(
                accessToken?.let { encryptor.encrypt(it) },
                refreshToken?.let { encryptor.encrypt(it) },
                expirationTimeMs,
                Instant.now()
            )
        }
    }

    fun merge(newCredential: GoogleCredential): GoogleCredential {
        return if (newCredential.refreshTokenEncrypted == null) {
            newCredential.copy(
                refreshTokenEncrypted = refreshTokenEncrypted,
                updatedAt = Instant.now()
            )
        } else {
            newCredential.copy(updatedAt = Instant.now())
        }
    }
}
