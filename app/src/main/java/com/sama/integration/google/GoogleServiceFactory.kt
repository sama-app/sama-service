package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.directory.Directory
import com.google.api.services.oauth2.Oauth2
import com.sama.SamaApplication
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.toStorageKey
import org.springframework.stereotype.Component

@Component
class GoogleServiceFactory(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleHttpTransport: HttpTransport,
    private val jsonFactory: JsonFactory,
) {

    fun calendarService(accountId: GoogleAccountId): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(accountId.toStorageKey())
        return Calendar.Builder(googleHttpTransport, jsonFactory, httpRequestInitializer(credential))
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    fun directoryService(accountId: GoogleAccountId): Directory {
        val credential = googleAuthorizationCodeFlow.loadCredential(accountId.toStorageKey())
        return Directory.Builder(googleHttpTransport, jsonFactory, httpRequestInitializer(credential))
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    fun oauth2Service(token: String): Oauth2 {
        val credential: GoogleCredential = GoogleCredential().setAccessToken(token)
        return Oauth2.Builder(googleHttpTransport, jsonFactory, httpRequestInitializer(credential))
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }

    private fun httpRequestInitializer(credential: HttpRequestInitializer): (request: HttpRequest) -> Unit =
        { request ->
            // Disable content logging for Google Api requests
            request.contentLoggingLimit = 0
            credential.initialize(request)
        }
}