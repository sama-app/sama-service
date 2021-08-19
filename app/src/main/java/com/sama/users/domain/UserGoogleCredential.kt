package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.ValueObject
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable
import org.springframework.security.crypto.encrypt.TextEncryptor

@DomainEntity
data class UserGoogleCredential(
    val userId: UserId,
    val googleCredential: GoogleCredential
)

/**
 * Object to store Google access & refresh tokens. Fields are marked nullable to catch cases
 * where access token is removed when Sama app permissions are removed.
 */
@ValueObject
@Embeddable
data class GoogleCredential private constructor (

    @Column(name = "google_access_token", table = "user_google_credential")
    val accessToken: String?,

    @Column(name = "google_refresh_token", table = "user_google_credential")
    val refreshToken: String?,

    @Column(name = "google_access_token_encrypted", table = "user_google_credential")
    val accessTokenEncrypted: String? = null,

    @Column(name = "google_refresh_token_encrypted", table = "user_google_credential")
    val refreshTokenEncrypted: String? = null,

    @Column(name = "google_token_expiration_time_ms", table = "user_google_credential")
    val expirationTimeMs: Long?,

    @Column(name = "updated_at", table = "user_google_credential")
    var updatedAt: Instant? = null
) {
    companion object {
        fun plainText(accessToken: String?, refreshToken: String?, expirationTimeMs: Long?): GoogleCredential {
            return GoogleCredential(accessToken, refreshToken, expirationTimeMs = expirationTimeMs)
        }

        fun encrypted(
            accessToken: String?,
            refreshToken: String?,
            expirationTimeMs: Long?,
            encryptor: TextEncryptor
        ): GoogleCredential {
            return GoogleCredential(
                accessToken,
                refreshToken,
                accessToken?.let { encryptor.encrypt(it) },
                refreshToken?.let { encryptor.encrypt(it) },
                expirationTimeMs
            )
        }
    }

    fun encrypt(encryptor: TextEncryptor): GoogleCredential {
        return copy(
            accessTokenEncrypted = accessToken?.let { encryptor.encrypt(it) },
            refreshTokenEncrypted = refreshToken?.let { encryptor.encrypt(it) },
        )
    }


    fun merge(newCredential: GoogleCredential): GoogleCredential {
        return if (newCredential.refreshToken == null || newCredential.refreshTokenEncrypted == null) {
            newCredential.copy(
                refreshToken = refreshToken,
                refreshTokenEncrypted = refreshTokenEncrypted,
                updatedAt = Instant.now()
            )
        } else {
            newCredential.copy(updatedAt = Instant.now())
        }
    }
}
