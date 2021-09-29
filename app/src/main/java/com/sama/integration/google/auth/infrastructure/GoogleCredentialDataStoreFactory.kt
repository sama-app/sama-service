package com.sama.integration.google.auth.infrastructure

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import java.io.Serializable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.crypto.encrypt.TextEncryptor

class GoogleCredentialDataStoreFactory(
    private val tokenEncryptor: TextEncryptor,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : DataStoreFactory {

    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        @Suppress("UNCHECKED_CAST")
        return EncryptedGoogleCredentialDataStore(
            tokenEncryptor,
            namedParameterJdbcTemplate,
            id,
            this
        ) as DataStore<V>
    }
}