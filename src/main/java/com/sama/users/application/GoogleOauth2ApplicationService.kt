package com.sama.users.application

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.*
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class GoogleOauth2ApplicationService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userSettingsDefaultsRepository: UserSettingsDefaultsRepository,
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock
) {
    fun beginGoogleOauth2(redirectUri: String): GoogleOauth2Redirect {
        val authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            .build()
        return GoogleOauth2Redirect(authorizationUrl)
    }

    fun processGoogleOauth2(
        redirectUri: String,
        authorizationCode: String?,
        authorizationError: String?
    ): GoogleOauth2Response {
        return when {
            authorizationCode != null -> completeGoogleOauth2(redirectUri, authorizationCode)
            authorizationError != null -> GoogleOauth2Failure("unhandled error code")
            else -> GoogleOauth2Failure("no code or error")
        }
    }

    private fun completeGoogleOauth2(redirectUri: String, authorizationCode: String): GoogleOauth2Success {
        // verifyAuthorizationCode
        // onAuthorizationTokenVerified(email) -> createUser
        // onUserCreated(userId) -> storeGoogleCredentials(token)
        // onGoogleCredentialsInitialized(userId) -> loadDefaultUserSettings(userId)


        return kotlin.runCatching {
            googleAuthorizationCodeFlow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute()
        }
            .mapCatching {
                val email = it["id_token"]
                    .let { it as String? }
                    .let { googleIdTokenVerifier.verify(it) }.payload.email
                    ?: throw InvalidEmailException()
                Pair(it, email)
            }
            .mapCatching {
                val authUser = userRepository.findByEmail(email = it.second)
                    ?: userRepository.save(User(email = it.second))
                Pair(it.first, authUser)
            }
            .onSuccess {
                val userId = it.second.id()!!
                googleAuthorizationCodeFlow.createAndStoreCredential(it.first, userId.toString())

                val userSettingsDefaults = userSettingsDefaultsRepository.findOne(userId)
                val userSettings = UserSettings.createUsingDefaults(userId, userSettingsDefaults)
                userSettingsRepository.save(userSettings)

            }
            .map {
                val jwtPair = it.second.issueJwtPair(accessJwtConfiguration, refreshJwtConfiguration, clock)
                GoogleOauth2Success(jwtPair.accessToken, jwtPair.refreshToken)
            }
            .getOrThrow()
    }
}


sealed class GoogleOauth2Response
data class GoogleOauth2Success(val accessToken: String, val refreshToken: String) : GoogleOauth2Response()
data class GoogleOauth2Failure(val error: String) : GoogleOauth2Response()
data class GoogleOauth2Redirect(val authorizationUrl: String)