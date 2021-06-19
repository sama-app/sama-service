package com.sama

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.firebase.FirebaseApp
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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

@Configuration
@EnableAutoConfiguration
class AppTestConfiguration {

    @Bean
    fun fixedClock(): Clock {
        val fixedDate = LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return Clock.fixed(fixedDate, ZoneId.systemDefault());
    }
}