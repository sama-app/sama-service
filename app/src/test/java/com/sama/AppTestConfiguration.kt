package com.sama

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
@EnableAutoConfiguration
class AppTestConfiguration {

    @Bean
    @Primary
    fun clock(): Clock {
        val fixedDate = LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return Clock.fixed(fixedDate, ZoneId.of("UTC"));
    }
}

@TestConfiguration
class IntegrationOverrides {
    @Bean
    fun googleClientSecrets(): GoogleClientSecrets {
        return GoogleClientSecrets()
    }

    @Bean
    fun googleAuthorizationCodeFlow(): GoogleAuthorizationCodeFlow {
        return Mockito.mock(GoogleAuthorizationCodeFlow::class.java)
    }

    @Bean
    fun firebaseCredentials(): GoogleCredentials {
        return Mockito.mock(GoogleCredentials::class.java)
    }

    @Bean
    fun firebaseAdminSdk(): FirebaseApp {
        return Mockito.mock(FirebaseApp::class.java)
    }
}