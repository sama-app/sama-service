package com.sama.auth.application

import com.sama.users.domain.GoogleCredential


data class VerifiedGoogleOauth2Token(
    val email: String,
    val credential: GoogleCredential
)

data class GoogleSignInCommand(val authCode: String)
sealed class GoogleOauth2Response
data class GoogleSignSuccessDTO(val accessToken: String, val refreshToken: String) : GoogleOauth2Response()
data class GoogleSignFailureDTO(val error: String) : GoogleOauth2Response()
