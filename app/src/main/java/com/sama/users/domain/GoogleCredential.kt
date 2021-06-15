package com.sama.users.domain

import com.sama.common.ValueObject
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable

@ValueObject
@Embeddable
data class GoogleCredential(

    @Column(name = "google_access_token", table = "user_google_credential")
    val accessToken: String,

    @Column(name = "google_refresh_token", table = "user_google_credential")
    val refreshToken: String?,

    @Column(name = "google_token_expiration_time_ms", table = "user_google_credential")
    val expirationTimeMs: Long,

    @Column(name = "updated_at", table = "user_google_credential")
    var updatedAt: Instant? = null
) {

    fun merge(credential: GoogleCredential): GoogleCredential {
        return if (credential.refreshToken == null) {
            credential.copy(updatedAt = Instant.now())
        } else {
            credential.copy(refreshToken = refreshToken, updatedAt = Instant.now())
        }
    }
}
