package com.sama.auth.application

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.sama.auth.domain.Oauth2State
import com.sama.auth.domain.Oauth2StateRepository
import com.sama.auth.domain.STATE_REGISTER
import com.sama.auth.domain.isLinkAccountOauth2State
import com.sama.auth.domain.isRegisterState
import com.sama.auth.domain.toLinkAccountOauth2State
import com.sama.auth.domain.toLinkAccountOauth2UserId
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.integration.google.auth.application.GoogleAccountService
import com.sama.integration.google.auth.application.GoogleOauth2Credential
import com.sama.integration.google.auth.application.LinkGoogleAccountCommand
import com.sama.users.application.GoogleOauth2Redirect
import com.sama.users.application.RegisterUserCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.application.UserSettingsApplicationService
import com.sama.users.domain.UserAlreadyExistsException
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentryTransaction
import java.time.Instant
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@ApplicationService
@Service
class GoogleOauth2ApplicationService(
    private val userApplicationService: UserApplicationService,
    private val userSettingsApplicationService: UserSettingsApplicationService,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val googleAccountService: GoogleAccountService,
    private val oauth2StateRepository: Oauth2StateRepository,
    @Value("\${integration.google.scopes}") private val requiredGoogleOauthScopes: List<String>,
) {
    private val logger = LogFactory.getLog(javaClass)

    fun generateAuthorizationUrl(redirectUri: String, userId: UserId? = null): GoogleOauth2Redirect {
        // Pass in user public identifier as "State" to link the authorized Google Account to
        // an existing Sama account
        val stateValue = when {
            userId != null -> userId.toLinkAccountOauth2State()
            else -> STATE_REGISTER
        }

        val oauth2State = Oauth2State(value = stateValue)
        val stateKey = oauth2StateRepository.save(oauth2State).key!!

        val authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            .setState(stateKey)
        authorizationUrl.set("include_granted_scopes", true)
        authorizationUrl.set("prompt", "select_account")
        return GoogleOauth2Redirect(authorizationUrl.build())
    }

    fun processOauth2Callback(redirectUri: String, code: String?, error: String?, state: String?): GoogleOauth2Response {
        val oauth2State = state?.let { oauth2StateRepository.findByKey(it) }
        require(oauth2State != null) { "Invalid Oauth2 state" }
        oauth2StateRepository.delete(oauth2State)

        val stateValue = oauth2State.value
        // If there is no state value -> initiate registration flow
        // If there is state value -> validate that it's a userId and link the Google Account to it
        return when {
            stateValue.isRegisterState() ->
                when {
                    code != null -> try {
                        registerUser(redirectUri, code)
                    } catch (e: MissingScopesException) {
                        GoogleSignErrorDTO("google_insufficient_permissions")
                    } catch (e: Exception) {
                        logger.warn("oauth2-callback-exception: $e.message")
                        GoogleSignErrorDTO("internal")
                    }
                    error != null -> {
                        logger.warn("oauth2-callback-error: $error")
                        GoogleSignErrorDTO("google_insufficient_permissions")
                    }
                    else -> GoogleSignErrorDTO("sama-invalid-oauth-callback")
                }
            stateValue.isLinkAccountOauth2State() -> {
                val userId = stateValue.toLinkAccountOauth2UserId()
                when {
                    code != null -> try {
                        linkGoogleAccount(redirectUri, code, userId)
                    } catch (e: MissingScopesException) {
                        LinkGoogleAccountErrorDTO("google_insufficient_permissions")
                    } catch (e: Exception) {
                        logger.warn("oauth2-callback-exception: $e.message")
                        LinkGoogleAccountErrorDTO("internal")
                    }
                    error != null -> {
                        logger.warn("oauth2-callback-error: $error")
                        LinkGoogleAccountErrorDTO("google_insufficient_permissions")
                    }
                    else -> LinkGoogleAccountErrorDTO("sama-invalid-oauth-callback")
                }
            }
            else -> throw IllegalArgumentException("Invalid OAuth2 state")
        }
    }

    fun googleSignIn(command: GoogleSignInCommand): GoogleSignSuccessDTO {
        try {
            return registerUser(null, command.authCode)
        } catch (ex: MissingScopesException) {
            logger.warn("google sign-in missing scopes")
            throw GoogleInsufficientPermissionsException(ex)
        }
    }

    private fun registerUser(redirectUri: String?, authorizationCode: String): GoogleSignSuccessDTO {
        // Step #1: Verify Google Code and Scopes
        val verifiedToken = verifyGoogleOauth2Code(authorizationCode, redirectUri)

        // Step #2: Fetch extended user details
        val googleUser = googleAccountService.getUserInfo(verifiedToken.credential.accessToken!!)

        // Step #3: Register a user or refresh their credentials if already registered
        val userId = kotlin.runCatching {
            val userId = userApplicationService.registerUser(RegisterUserCommand(verifiedToken.email, googleUser.fullName))
            googleAccountService.linkAccount(userId, LinkGoogleAccountCommand(verifiedToken.email, verifiedToken.credential))
            userSettingsApplicationService.createUserSettings(userId)
            userId
        }.recover {
            if (it !is UserAlreadyExistsException) {
                throw it
            }
            val userId = userApplicationService.findInternalByEmail(verifiedToken.email).id
            googleAccountService.linkAccount(userId, LinkGoogleAccountCommand(verifiedToken.email, verifiedToken.credential))
            userId
        }.getOrThrow()

        // Step #4: Issue SAMA authentication tokens
        val (accessToken, refreshToken) = userApplicationService.issueTokens(userId)
        return GoogleSignSuccessDTO(accessToken, refreshToken)
    }

    private fun linkGoogleAccount(redirectUri: String?, authorizationCode: String, userId: UserId): LinkGoogleAccountSuccessDTO {
        // Step #1: Verify Google Code and Scopes
        val verifiedToken = verifyGoogleOauth2Code(authorizationCode, redirectUri)

        // Step #2: Link Google Account
        val googleAccountPublicId = googleAccountService.linkAccount(
            userId,
            LinkGoogleAccountCommand(verifiedToken.email, verifiedToken.credential)
        )

        return LinkGoogleAccountSuccessDTO(googleAccountPublicId)
    }

    private fun verifyGoogleOauth2Code(authorizationCode: String, redirectUri: String?): VerifiedGoogleOauth2Token {
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
            VerifiedGoogleOauth2Token(
                email,
                GoogleOauth2Credential(it.accessToken, it.refreshToken, it.expiresInSeconds)
            )
        }.getOrThrow()
        return verifiedToken
    }

    @SentryTransaction(operation = "deleteOauth2States")
    @Scheduled(cron = "0 0 * * * *")
    fun expireMeetings() {
        oauth2StateRepository.deleteByCreatedAtLessThan(Instant.now().minusSeconds(600))
    }
}