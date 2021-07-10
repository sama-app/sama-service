package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.oauth2.Oauth2
import com.sama.SamaApplication
import com.sama.users.domain.BasicUserDetails
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class GoogleUserRepository(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleNetHttpTransport: HttpTransport,
    private val googleJacksonFactory: JacksonFactory
) {

    fun findUsingToken(accessToken: String): BasicUserDetails {
        val result = oauth2Service(accessToken).userinfo().get().execute()
        return result.let { BasicUserDetails(it.email, it.name) }
    }

    fun find(userId: UserId): BasicUserDetails {
        val result = oauth2ServiceForUser(userId).userinfo().get().execute()
        return result.let { BasicUserDetails(it.email, it.name) }
    }

    private fun oauth2ServiceForUser(userId: Long): Oauth2 {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Oauth2.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    private fun oauth2Service(token: String): Oauth2 {
        val credential: GoogleCredential = GoogleCredential().setAccessToken(token)
        return Oauth2.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }
}