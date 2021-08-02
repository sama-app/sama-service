package com.sama.users.application

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleUserRepository
import com.sama.users.domain.GoogleCredential
import com.sama.users.domain.UserAlreadyExistsException
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@ApplicationService
@Service
class GoogleOauth2ApplicationService(
    private val userApplicationService: UserApplicationService,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleUserRepository: GoogleUserRepository,
    @Value("\${integration.google.scopes}") private val requiredGoogleOauthScopes: List<String>
) {
    private val logger = LogFactory.getLog(javaClass)

    fun beginGoogleOauth2(redirectUri: String): GoogleOauth2Redirect {
        val authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
        authorizationUrl.set("include_granted_scopes", true)
        return GoogleOauth2Redirect(authorizationUrl.build())
    }

    fun processGoogleOauth2(redirectUri: String, code: String?, error: String?): GoogleOauth2Response {
        return when {
            code != null -> try {
                processGoogleOauth2AuthCode(redirectUri, code)
            } catch (e: MissingScopesException) {
                GoogleOauth2Failure("google_insufficient_permissions")
            } catch (e: Exception) {
                logger.warn("oauth2-callback-exception: $e.message")
                GoogleOauth2Failure("internal")
            }
            error != null -> {
                logger.warn("oauth2-callback-error-callback: $error")
                GoogleOauth2Failure("google_insufficient_permissions")
            }
            else -> GoogleOauth2Failure("sama-invalid-oauth-callback")
        }
    }

    private fun processGoogleOauth2AuthCode(redirectUri: String, authorizationCode: String): GoogleOauth2Success {
        // Step #1: Verify Google Code and Scopes
        val verifiedToken = kotlin.runCatching {
            googleAuthorizationCodeFlow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute()
        }.mapCatching {
            val acquiredScopes = it.scope.split(" ")
            if (!acquiredScopes.containsAll(requiredGoogleOauthScopes)) {
                throw MissingScopesException()
            }

            val parseIdToken = it.parseIdToken()
            val email = parseIdToken.payload.email
            VerifiedGoogleOauth2Token(email, GoogleCredential(it.accessToken, it.refreshToken, it.expiresInSeconds))
        }.getOrThrow()

        // Step #2: Fetch extended user details
        val userDetails = googleUserRepository.findUsingToken(verifiedToken.credential.accessToken!!)

        // Step #3: Register a user with
        // settings or refresh credentials
        val userId = kotlin.runCatching {
            val userId = userApplicationService.registerUser(
                RegisterUserCommand(verifiedToken.email, userDetails.fullName, verifiedToken.credential)
            )
            userApplicationService.createUserSettings(userId)
            userId
        }.recover {
            if (it !is UserAlreadyExistsException) {
                throw it
            }
            val userId = userApplicationService.refreshCredentials(
                RefreshCredentialsCommand(
                    verifiedToken.email,
                    verifiedToken.credential
                )
            )

            userApplicationService.updateBasicDetails(
                userId,
                UpdateUserBasicDetailsCommand(userDetails.fullName)
            )

            userId
        }.getOrThrow()

        // Step #4: Issue SAMA authentication tokens
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

class MissingScopesException :
    RuntimeException("User did not grant all required scopes")