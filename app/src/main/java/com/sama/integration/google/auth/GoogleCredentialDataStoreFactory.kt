package com.sama.integration.google.auth

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.sama.users.infrastructure.jpa.UserJpaRepository
import java.io.Serializable
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Component

class GoogleCredentialDataStoreFactory(
    private val userRepository: UserJpaRepository,
    private val tokenEncryptor: TextEncryptor,
) : DataStoreFactory {

    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        @Suppress("UNCHECKED_CAST")
        return EncryptedGoogleCredentialDataStore(
            id,
            userRepository,
            tokenEncryptor,
            this
        ) as DataStore<V>
    }
}