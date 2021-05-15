package com.sama.integration.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.sama.SamaApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileNotFoundException

@Configuration
class FirebaseConfiguration {

    private val FIREBASE_CREDENTIALS_FILE_PATH = "/secrets/firebase-adminsdk-credentials.json"

    @Bean
    fun firebaseAdminSdk(): FirebaseApp {
        val serviceAccount = SamaApplication::class.java.getResourceAsStream(FIREBASE_CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $FIREBASE_CREDENTIALS_FILE_PATH")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.initializeApp(options)
    }
}