package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.sama.users.domain.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.StringReader


@Configuration
class GoogleConfiguration() {

    @Bean
    fun googleJacksonFactory(): JacksonFactory {
        return JacksonFactory.getDefaultInstance()
    }

    @Bean
    fun googleNetHttpTransport(): NetHttpTransport {
        return GoogleNetHttpTransport.newTrustedTransport()
    }

    @Bean
    @Profile("!ci")
    fun googleClientSecrets(@Value("\${integration.google.credentials}") credentials: String): GoogleClientSecrets {
        return GoogleClientSecrets.load(googleJacksonFactory(), StringReader(credentials))
    }

    @Bean
    @Profile("!ci")
    fun googleAuthorizationCodeFlow(
        @Value("\${integration.google.scopes}") scopes: List<String>,
        googleClientSecrets: GoogleClientSecrets,
        userRepository: UserRepository
    ): GoogleAuthorizationCodeFlow {
        return GoogleAuthorizationCodeFlow.Builder(
            googleNetHttpTransport(),
            googleJacksonFactory(),
            googleClientSecrets,
            scopes
        )
            .setDataStoreFactory(GoogleCredentialJPADataStoreFactory(userRepository))
            .setAccessType("offline")
            .setApprovalPrompt("auto")
            .build()
    }

    @Bean
    fun googleIdTokenVerifier(@Value("\${integration.google.client-id}") clientId: String): GoogleIdTokenVerifier {
        return GoogleIdTokenVerifier.Builder(
            googleNetHttpTransport(),
            googleJacksonFactory()
        )
            .setAudience(listOf(clientId))
            .build()
    }
}