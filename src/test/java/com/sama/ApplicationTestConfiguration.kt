package com.sama

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.auth.oauth2.GoogleCredentials
import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("ci")
class IntegrationOverrides {

    @Bean
    @Primary
    fun googleAuthorizationCodeFlow(): GoogleAuthorizationCodeFlow {
        return mock(GoogleAuthorizationCodeFlow::class.java)
    }

    @Bean
    @Primary
    fun firebaseCredentials(): GoogleCredentials {
        return mock(GoogleCredentials::class.java)
    }
}