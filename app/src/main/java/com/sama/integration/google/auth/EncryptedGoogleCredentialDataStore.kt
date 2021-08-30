package com.sama.integration.google.auth

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.AbstractDataStore
import com.google.api.client.util.store.DataStore
import com.sama.common.findByIdOrThrow
import com.sama.integration.google.GoogleInvalidCredentialsException
import com.sama.users.infrastructure.jpa.GoogleCredential
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.jpa.applyChanges
import org.apache.commons.logging.LogFactory
import org.springframework.security.crypto.encrypt.TextEncryptor

class EncryptedGoogleCredentialDataStore internal constructor(
    dataStoreId: String,
    private val userRepository: UserJpaRepository,
    private val tokenEncryptor: TextEncryptor,
    dataStoreFactory: GoogleCredentialDataStoreFactory,
) : AbstractDataStore<StoredCredential>(dataStoreFactory, dataStoreId), DataStore<StoredCredential> {
    private val logger = LogFactory.getLog(EncryptedGoogleCredentialDataStore::class.java)

    override fun keySet(): Set<String> {
        return userRepository.findAllIds()
            .map { it.toString() }
            .toSet()
    }

    override fun get(id: String): StoredCredential? {
        val storedCredential = userRepository.findByIdOrThrow(id.toLong())
            .googleCredential
            ?.let { googleCredential ->
                val credential = StoredCredential()
                credential.accessToken = googleCredential.accessTokenEncrypted
                    ?.let { tokenEncryptor.decrypt(it) }
                credential.refreshToken = googleCredential.refreshTokenEncrypted
                    ?.let { tokenEncryptor.decrypt(it) }
                credential.expirationTimeMilliseconds = googleCredential.expirationTimeMs
                credential
            }
        return storedCredential
    }

    override fun set(id: String, c: StoredCredential): DataStore<StoredCredential> {
        val user = userRepository.findByIdOrThrow(id.toLong())
        val newCredentials = GoogleCredential.encrypted(
            c.accessToken,
            c.refreshToken,
            c.expirationTimeMilliseconds,
            tokenEncryptor
        )

        val googleCredential = user.googleCredential?.merge(newCredentials)
            ?: newCredentials

        user.applyChanges(googleCredential).also { userRepository.save(it) }

        // throw only after storing the invalid credentials so that Google's SDK
        // can handle the missing tokens itself
        if (!c.isUsable()) {
            logger.warn("User#$id received invalid Google Credentials")
            throw GoogleInvalidCredentialsException(null)
        }
        return this
    }

    private fun StoredCredential.isUsable(): Boolean {
        // access token sent as null when credentials are invalidated or permissions
        // are removed
        return accessToken != null
    }

    override fun values(): Collection<StoredCredential> {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun clear(): DataStore<StoredCredential> {
        throw UnsupportedOperationException("Unsupported: modify GoogleCredentials directly via UserEntity")
    }

    override fun delete(id: String): DataStore<StoredCredential> {
        throw UnsupportedOperationException("Unsupported: modify GoogleCredentials directly via UserEntity")
    }
}