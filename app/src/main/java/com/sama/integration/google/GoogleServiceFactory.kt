package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.oauth2.Oauth2
import com.sama.SamaApplication
import org.springframework.stereotype.Component

@Component
class GoogleServiceFactory(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleNetHttpTransport: HttpTransport,
    private val googleJacksonFactory: JacksonFactory
) {

    fun calendarService(userId: Long): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Calendar.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    fun oauth2Service(userId: Long): Oauth2 {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Oauth2.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    fun oauth2Service(token: String): Oauth2 {
        val credential: GoogleCredential = GoogleCredential().setAccessToken(token)
        return Oauth2.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }
}