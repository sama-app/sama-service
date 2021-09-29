package com.sama.auth.application

import com.sama.integration.google.auth.application.GoogleOauth2Credential
import com.sama.integration.google.auth.domain.GoogleAccountPublicId


data class VerifiedGoogleOauth2Token(
    val email: String,
    val credential: GoogleOauth2Credential,
)

data class GoogleSignInCommand(val authCode: String)

sealed class GoogleOauth2Response
data class GoogleSignSuccessDTO(val accessToken: String, val refreshToken: String) : GoogleOauth2Response()
data class GoogleSignErrorDTO(val error: String) : GoogleOauth2Response()
data class LinkGoogleAccountSuccessDTO(val googleAccountId: GoogleAccountPublicId) : GoogleOauth2Response()
data class LinkGoogleAccountErrorDTO(val error: String) : GoogleOauth2Response()