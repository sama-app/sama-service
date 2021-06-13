package com.sama.integration.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FirebaseConfiguration {

    @Bean
    @Profile("!ci")
    fun firebaseCredentials(@Value("\${integration.firebase.credentials}") credentials: String): GoogleCredentials {
        return GoogleCredentials.fromStream(credentials.byteInputStream())
    }

    @Bean
    fun firebaseAdminSdk(firebaseCredentials: GoogleCredentials): FirebaseApp {
        val options = FirebaseOptions.builder()
            .setCredentials(firebaseCredentials)
            .build()

        return FirebaseApp.initializeApp(options)
    }
}