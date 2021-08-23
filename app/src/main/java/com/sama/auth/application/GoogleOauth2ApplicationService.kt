package com.sama.auth.application

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.integration.google.GoogleUserRepository
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.application.RefreshCredentialsCommand
import com.sama.users.application.RegisterUserCommand
import com.sama.users.application.UpdateUserPublicDetailsCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.application.UserSettingsApplicationService
import com.sama.users.domain.GoogleCredential
import com.sama.users.domain.UserAlreadyExistsException
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@ApplicationService
@Service
class GoogleOauth2ApplicationService(
    private val userApplicationService: UserApplicationService,
    private val userSettingsApplicationService: UserSettingsApplicationService,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val googleUserRepository: GoogleUserRepository,
    @Value("\${integration.google.scopes}") private val requiredGoogleOauthScopes: List<String>
) {
    private val logger = LogFactory.getLog(javaClass)

    fun googleSignIn(command: GoogleSignInCommand): GoogleSignSuccessDTO {
        try {
            return processGoogleOauth2AuthCode(null, command.authCode)
        } catch (ex: MissingScopesException) {
            logger.warn("google sign-in missing scopes")
            throw GoogleInsufficientPermissionsException(ex)
        }
    }

    fun beginGoogleWebOauth2(redirectUri: String): GoogleOauth2Redirect {
        val authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
        authorizationUrl.set("include_granted_scopes", true)
        return GoogleOauth2Redirect(authorizationUrl.build())
    }

    fun processGoogleWebOauth2(redirectUri: String, code: String?, error: String?): GoogleOauth2Response {
        return when {
            code != null -> try {
                processGoogleOauth2AuthCode(redirectUri, code)
            } catch (e: MissingScopesException) {
                GoogleSignFailureDTO("google_insufficient_permissions")
            } catch (e: Exception) {
                logger.warn("oauth2-callback-exception: $e.message")
                GoogleSignFailureDTO("internal")
            }
            error != null -> {
                logger.warn("oauth2-callback-error-callback: $error")
                GoogleSignFailureDTO("google_insufficient_permissions")
            }
            else -> GoogleSignFailureDTO("sama-invalid-oauth-callback")
        }
    }

    private fun processGoogleOauth2AuthCode(redirectUri: String?, authorizationCode: String): GoogleSignSuccessDTO {
        // Step #1: Verify Google Code and Scopes
        val verifiedToken = kotlin.runCatching {
            val tokenRequest = googleAuthorizationCodeFlow.newTokenRequest(authorizationCode)
            tokenRequest
                .setRedirectUri(redirectUri ?: "") // empty string for non-web applications
                .execute()
        }.mapCatching {
            val acquiredScopes = it.scope.split(" ")
            if (!acquiredScopes.containsAll(requiredGoogleOauthScopes)) {
                throw MissingScopesException()
            }

            val parsedIdToken = it.parseIdToken()
            if (!googleIdTokenVerifier.verify(parsedIdToken)) {
                throw RuntimeException("Invalid Google ID token")
            }
            val email = parsedIdToken.payload.email
            VerifiedGoogleOauth2Token(email, GoogleCredential.plainText(it.accessToken, it.refreshToken, it.expiresInSeconds))
        }.getOrThrow()

        // Step #2: Fetch extended user details
        val userDetails = googleUserRepository.findUsingToken(verifiedToken.credential.accessToken!!)

        // Step #3: Register a user with
        // settings or refresh credentials
        val userId = kotlin.runCatching {
            val userId = userApplicationService.registerUser(
                RegisterUserCommand(verifiedToken.email, userDetails.fullName, verifiedToken.credential)
            )
            userSettingsApplicationService.createUserSettings(userId)
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

            userApplicationService.updatePublicDetails(
                userId,
                UpdateUserPublicDetailsCommand(userDetails.fullName)
            )

            userId
        }.getOrThrow()

        // Step #4: Issue SAMA authentication tokens
        val (accessToken, refreshToken) = userApplicationService.issueTokens(userId)
        return GoogleSignSuccessDTO(accessToken, refreshToken)
    }
}