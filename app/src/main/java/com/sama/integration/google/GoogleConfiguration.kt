package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.sama.integration.google.auth.infrastructure.GoogleCredentialDataStoreFactory
import java.io.StringReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor


@Configuration
class GoogleConfiguration() {

    @Bean
    fun googleJsonFactory(): JsonFactory {
        return GsonFactory.getDefaultInstance()
    }

    @Bean
    fun googleHttpTransport(): HttpTransport {
        return ApacheHttpTransport()
    }

    @Bean
    @Profile("!ci")
    fun googleClientSecrets(@Value("\${integration.google.credentials}") credentials: String): GoogleClientSecrets {
        return GoogleClientSecrets.load(googleJsonFactory(), StringReader(credentials))
    }

    @Bean
    fun googleTokenEncryptor(
        @Value("\${integration.google.tokens.encryption.password}") encryptionPassword: String,
        @Value("\${integration.google.tokens.encryption.salt}") encryptionSalt: String,
    ): TextEncryptor {
        check(encryptionPassword.isNotBlank() && encryptionSalt.isNotBlank()) { "Invalid TokenEncryptor configuration" }
        return Encryptors.text(encryptionPassword, encryptionSalt)
    }

    @Bean
    fun googleCredentialDataStoreFactory(
        jdbcTemplate: NamedParameterJdbcTemplate,
        googleTokenEncryptor: TextEncryptor,
    ): GoogleCredentialDataStoreFactory {
        return GoogleCredentialDataStoreFactory(googleTokenEncryptor, jdbcTemplate)
    }

    @Bean
    @Profile("!ci")
    fun googleAuthorizationCodeFlow(
        @Value("\${integration.google.scopes}") scopes: List<String>,
        googleClientSecrets: GoogleClientSecrets,
        googleCredentialDataStoreFactory: GoogleCredentialDataStoreFactory,
    ): GoogleAuthorizationCodeFlow {
        return GoogleAuthorizationCodeFlow.Builder(
            googleHttpTransport(),
            googleJsonFactory(),
            googleClientSecrets,
            scopes
        )
            .setDataStoreFactory(googleCredentialDataStoreFactory)
            .setAccessType("offline")
            .build()
    }

    @Bean
    fun googleIdTokenVerifier(@Value("\${integration.google.client-id}") clientId: String): GoogleIdTokenVerifier {
        return GoogleIdTokenVerifier.Builder(googleHttpTransport(), googleJsonFactory())
            .setAudience(listOf(clientId))
            .build()
    }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.google.sync.channel")
class ChannelConfiguration(
    val enabled: Boolean,
    val callbackUrl: String,
    val expiresInHours: Long,
    val cleanupLeadTimeHours: Long
)