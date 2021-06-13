package com.sama

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.firebase.FirebaseApp
import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IntegrationOverrides {

    @Bean
    fun googleAuthorizationCodeFlow(): GoogleAuthorizationCodeFlow {
        return mock(GoogleAuthorizationCodeFlow::class.java)
    }

    @Bean
    fun firebaseAdminSdk(): FirebaseApp {
        return mock(FirebaseApp::class.java)
    }
}