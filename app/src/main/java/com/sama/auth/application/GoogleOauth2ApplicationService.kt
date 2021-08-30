package com.sama.auth.application

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.integration.google.auth.GoogleCredentialDataStoreFactory
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.user.GoogleUserService
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.application.RegisterUserCommand
import com.sama.users.application.UpdateUserPublicDetailsCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.application.UserSettingsApplicationService
import com.sama.users.domain.UserAlreadyExistsException
import com.sama.users.domain.UserId
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@ApplicationService
@Service
class GoogleOauth2ApplicationService(
    private val userApplicationService: UserApplicationService,
    private val userSettingsApplicationService: UserSettingsApplicationService,
    private val googleCredentialStoreFactory: GoogleCredentialDataStoreFactory,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val googleUserService: GoogleUserService,
    private val googleCalendarService: GoogleCalendarService,
    @Value("\${integration.google.scopes}") private val requiredGoogleOauthScopes: List<String>,
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
            VerifiedGoogleOauth2Token(email,
                GoogleOauth2Credential(it.accessToken, it.refreshToken, it.expiresInSeconds))
        }.getOrThrow()

        // Step #2: Fetch extended user details
        val googleUser = googleUserService.findUsingToken(verifiedToken.credential.accessToken!!)

        // Step #3: Register a user with
        // settings or refresh credentials
        val userId = kotlin.runCatching {
            val userId = userApplicationService.registerUser(
                RegisterUserCommand(verifiedToken.email, googleUser.fullName)
            )
            persistGoogleCredentials(userId, verifiedToken)
            googleCalendarService.enableCalendarSync(userId)
            userSettingsApplicationService.createUserSettings(userId)
            userId
        }.recover {
            if (it !is UserAlreadyExistsException) {
                throw it
            }
            val userId = userApplicationService.findInternalByEmail(verifiedToken.email).id
            persistGoogleCredentials(userId, verifiedToken)
            googleCalendarService.enableCalendarSync(userId)
            userApplicationService.updatePublicDetails(userId, UpdateUserPublicDetailsCommand(googleUser.fullName))
            userId
        }.getOrThrow()

        // Step #4: Issue SAMA authentication tokens
        val (accessToken, refreshToken) = userApplicationService.issueTokens(userId)
        return GoogleSignSuccessDTO(accessToken, refreshToken)
    }

    private fun persistGoogleCredentials(userId: UserId, verifiedToken: VerifiedGoogleOauth2Token) {
        val credentialStore = googleCredentialStoreFactory.getDataStore<StoredCredential>(userId.id.toString())
        credentialStore.set(userId.id.toString(), StoredCredential().apply {
            accessToken = verifiedToken.credential.accessToken
            refreshToken = verifiedToken.credential.refreshToken
            expirationTimeMilliseconds = verifiedToken.credential.expirationTimeMs
        })
    }
}