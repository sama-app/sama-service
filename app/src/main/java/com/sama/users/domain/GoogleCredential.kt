package com.sama.users.domain

import com.sama.common.ValueObject
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * Object to store Google access & refresh tokens. Fields are marked nullable to catch cases
 * where access token is removed when Sama app permissions are removed.
 */
@ValueObject
@Embeddable
data class GoogleCredential(

    @Column(name = "google_access_token", table = "user_google_credential")
    val accessToken: String?,

    @Column(name = "google_refresh_token", table = "user_google_credential")
    val refreshToken: String?,

    @Column(name = "google_token_expiration_time_ms", table = "user_google_credential")
    val expirationTimeMs: Long?,

    @Column(name = "updated_at", table = "user_google_credential")
    var updatedAt: Instant? = null
) {

    fun merge(newCredential: GoogleCredential): GoogleCredential {
        return if (newCredential.refreshToken == null) {
            newCredential.copy(refreshToken = refreshToken, updatedAt = Instant.now())
        } else {
            newCredential.copy(updatedAt = Instant.now())
        }
    }
}
