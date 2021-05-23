package com.sama.users.application

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.sama.common.ApplicationService
import com.sama.users.domain.GoogleCredential
import com.sama.users.domain.UserAlreadyExistsException
import liquibase.pro.packaged.it
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Service

@ApplicationService
@Service
class GoogleOauth2ApplicationService(
    private val userApplicationService: UserApplicationService,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
) {
    private val logger = LogFactory.getLog(javaClass)

    fun beginGoogleOauth2(redirectUri: String): GoogleOauth2Redirect {
        val authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            .build()
        return GoogleOauth2Redirect(authorizationUrl)
    }

    fun processGoogleOauth2(redirectUri: String, code: String?, error: String?): GoogleOauth2Response {
        return when {
            code != null -> try {
                processGoogleOauth2AuthCode(redirectUri, code)
            } catch (e: Exception) {
                logger.warn("oauth2-callback-error: $e.message")
                GoogleOauth2Failure("sama-internal")
            }
            error != null -> GoogleOauth2Failure(error)
            else -> GoogleOauth2Failure("sama-invalid-oauth-callback")
        }
    }

    private fun processGoogleOauth2AuthCode(redirectUri: String, authorizationCode: String): GoogleOauth2Success {
        // Step #1: Verify Google Code
        val verifiedToken = kotlin.runCatching {
            googleAuthorizationCodeFlow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute()
        }.map {
            val email = it.parseIdToken().payload.email
            VerifiedGoogleOauth2Token(email, GoogleCredential(it.accessToken, it.refreshToken, it.expiresInSeconds))
        }.getOrThrow()


        // Step #2: Register a user with settings or refresh credentials
        val userId = kotlin.runCatching {
            val userId = userApplicationService.registerUser(
                RegisterUserCommand(verifiedToken.email, verifiedToken.credential)
            )
            userApplicationService.createUserSettings(userId)
            userId
        }.recover {
            if (it !is UserAlreadyExistsException) {
                throw it
            }
            userApplicationService.refreshCredentials(
                RefreshCredentialsCommand(
                    verifiedToken.email,
                    verifiedToken.credential
                )
            )
        }.getOrThrow()

        // Step #3: Issue authentication tokens
        val (accessToken, refreshToken) = userApplicationService.issueTokens(userId)
        return GoogleOauth2Success(accessToken, refreshToken)
    }
}

data class VerifiedGoogleOauth2Token(
    val email: String,
    val credential: GoogleCredential
)

sealed class GoogleOauth2Response
data class GoogleOauth2Success(val accessToken: String, val refreshToken: String) : GoogleOauth2Response()
data class GoogleOauth2Failure(val error: String) : GoogleOauth2Response()
