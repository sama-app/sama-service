package com.sama.integration.google.auth.application

import com.fasterxml.jackson.annotation.JsonCreator
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import java.util.UUID

data class LinkGoogleAccountCommand(val email: String, val credential: GoogleOauth2Credential)

data class UnlinkGoogleAccountCommand(val googleAccountId: GoogleAccountPublicId) {
    @JsonCreator // Jackson cannot de-serialize value classes just yet
    private constructor(googleAccountId: String) : this(GoogleAccountPublicId(UUID.fromString(googleAccountId)))
}

data class GoogleOauth2Credential(
    val accessToken: String?,
    val refreshToken: String?,
    val expirationTimeMs: Long?,
)