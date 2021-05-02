package com.sama.infrastructure.google

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.sama.auth.domain.AuthUserRepository
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class JPADataStoreFactory(private val authUserRepository: AuthUserRepository) : DataStoreFactory {

    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        @Suppress("UNCHECKED_CAST")
        return JPADataStore(id, authUserRepository, this) as DataStore<V>
    }
}