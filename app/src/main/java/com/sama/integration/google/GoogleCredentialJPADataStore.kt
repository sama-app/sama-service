package com.sama.integration.google

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.AbstractDataStore
import com.google.api.client.util.store.DataStore
import com.sama.common.findByIdOrThrow
import com.sama.users.domain.GoogleCredential
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import com.sama.users.domain.applyChanges
import org.apache.commons.logging.LogFactory
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import java.util.function.Function
import java.util.stream.Collectors

class GoogleCredentialJPADataStore internal constructor(
    dataStoreId: String,
    private val userRepository: UserRepository,
    dataStoreFactory: GoogleCredentialJPADataStoreFactory
) : AbstractDataStore<StoredCredential>(dataStoreFactory, dataStoreId), DataStore<StoredCredential> {
    private val logger = LogFactory.getLog(GoogleCredentialJPADataStore::class.java)

    override fun keySet(): Set<String> {
        return userRepository.findAllIds()
            .map { it.toString() }
            .toSet()
    }

    override fun values(): Collection<StoredCredential> {
        return userRepository.findAll().stream()
            .map(toStoredCredential)
            .collect(Collectors.toList())
    }

    override fun get(id: String): StoredCredential? {
        return userRepository.findByIdOrThrow(id.toLong())
            .googleCredential
            ?.let {
                val credential = StoredCredential()
                credential.accessToken = it.accessToken
                credential.refreshToken = it.refreshToken
                credential.expirationTimeMilliseconds = it.expirationTimeMs
                credential
            }
    }

    override fun set(id: String, c: StoredCredential): DataStore<StoredCredential> {
        if (!c.isUsable()) {
            logger.warn("User#$id received invalid Google Credentials")
            throw GoogleInvalidCredentialsException(id.toLong(), null)
        }

        val user = userRepository.findByIdOrThrow(id.toLong())

        val newCredentials = GoogleCredential(c.accessToken, c.refreshToken, c.expirationTimeMilliseconds)

        val googleCredential = user.googleCredential?.merge(newCredentials)
            ?: newCredentials

        user.applyChanges(googleCredential).also { userRepository.save(it) }
        return this
    }

    override fun clear(): DataStore<StoredCredential> {
        throw UnsupportedOperationException("Unsupported: modify GoogleCredentials directly via UserEntity")
    }

    override fun delete(id: String): DataStore<StoredCredential> {
        throw UnsupportedOperationException("Unsupported: modify GoogleCredentials directly via UserEntity")
    }

    private val toStoredCredential = Function { au: UserEntity ->
        val credential = StoredCredential()
        val gc: GoogleCredential = au.googleCredential!!
        credential.accessToken = gc.accessToken
        credential.refreshToken = gc.refreshToken
        credential.expirationTimeMilliseconds = gc.expirationTimeMs
        credential
    }

    private fun StoredCredential.isUsable(): Boolean {
        // access token sent as null when credentials are invalidated or permissions
        // are removed
        return accessToken != null
    }
}