package com.sama.infrastructure.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.oauth2.Oauth2Scopes
import com.google.common.collect.Lists
import com.sama.SamaApplication
import com.sama.auth.domain.AuthUserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileNotFoundException
import java.io.InputStreamReader

@Configuration
class GoogleConfiguration {
    private val SCOPES: List<String> = Lists.newArrayList(
        CalendarScopes.CALENDAR_READONLY,
        Oauth2Scopes.USERINFO_EMAIL,
        Oauth2Scopes.USERINFO_PROFILE
    )
    private val CLIENT_ID = "690866307711-8nm12p73mo585k5njoaqepjupgm31im3.apps.googleusercontent.com"
    private val CREDENTIALS_FILE_PATH = "/credentials.json"

    @Bean
    fun googleJacksonFactory(): JacksonFactory {
        return JacksonFactory.getDefaultInstance()
    }

    @Bean
    fun googleClientSecrets(): GoogleClientSecrets {
        val credentialsFile = SamaApplication::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val jsonFactory = JacksonFactory.getDefaultInstance()
        return GoogleClientSecrets.load(jsonFactory, InputStreamReader(credentialsFile))
    }

    @Bean
    fun googleNetHttpTransport(): NetHttpTransport {
        return GoogleNetHttpTransport.newTrustedTransport()
    }

    @Bean
    fun googleAuthorizationCodeFlow(authUserRepository: AuthUserRepository): GoogleAuthorizationCodeFlow {
        return GoogleAuthorizationCodeFlow.Builder(
            googleNetHttpTransport(),
            googleJacksonFactory(),
            googleClientSecrets(),
            SCOPES
        )
            .setDataStoreFactory(JPADataStoreFactory(authUserRepository))
            .setAccessType("offline")
            .build()
    }

    @Bean
    fun googleIdTokenVerifier(): GoogleIdTokenVerifier {
        return GoogleIdTokenVerifier.Builder(
            googleNetHttpTransport(),
            googleJacksonFactory()
        )
            .setAudience(listOf(CLIENT_ID))
            .build()
    }
}