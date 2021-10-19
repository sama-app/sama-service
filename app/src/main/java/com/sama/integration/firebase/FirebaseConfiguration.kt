package com.sama.integration.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.sama.common.spring.LoggingRequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class FirebaseConfiguration {

    @Bean
    @Profile("!ci")
    fun firebaseCredentials(@Value("\${integration.firebase.credentials}") credentials: String): GoogleCredentials {
        return GoogleCredentials.fromStream(credentials.byteInputStream())
    }

    @Bean
    @Profile("!ci")
    fun firebaseAdminSdk(firebaseCredentials: GoogleCredentials): FirebaseApp {
        val options = FirebaseOptions.builder()
            .setCredentials(firebaseCredentials)
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseRestTemplate() = RestTemplate().apply {
        requestFactory = HttpComponentsClientHttpRequestFactory()
        interceptors = listOf(LoggingRequestInterceptor())
    }

    @Bean
    fun firebaseDynamicLinkService(
        @Value("\${integration.firebase.api-key}") apiKey: String,
        dynamicLinkConfiguration: FirebaseDynamicLinkConfiguration,
        dynamicLinkRepository: FirebaseDynamicLinkRepository,
    ) = FirebaseDynamicLinkService(firebaseRestTemplate(), dynamicLinkRepository, dynamicLinkConfiguration, apiKey)
}