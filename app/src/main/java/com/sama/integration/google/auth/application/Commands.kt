package com.sama.integration.google.auth.application

import com.sama.integration.google.auth.domain.GoogleAccountPublicId

data class LinkGoogleAccountCommand(val email: String, val credential: GoogleOauth2Credential)

data class UnlinkGoogleAccountCommand(val googleAccountId: GoogleAccountPublicId)

data class GoogleOauth2Credential(
    val accessToken: String?,
    val refreshToken: String?,
    val expirationTimeMs: Long?,
)