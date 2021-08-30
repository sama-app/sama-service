package com.sama.auth.application


data class VerifiedGoogleOauth2Token(
    val email: String,
    val credential: GoogleOauth2Credential,
)

data class GoogleOauth2Credential(
    val accessToken: String?,
    val refreshToken: String?,
    val expirationTimeMs: Long?,
)

data class GoogleSignInCommand(val authCode: String)
sealed class GoogleOauth2Response
data class GoogleSignSuccessDTO(val accessToken: String, val refreshToken: String) : GoogleOauth2Response()
data class GoogleSignFailureDTO(val error: String) : GoogleOauth2Response()
