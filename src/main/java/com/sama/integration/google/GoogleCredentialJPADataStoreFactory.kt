package com.sama.integration.google

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class GoogleCredentialJPADataStoreFactory(private val userRepository: UserRepository) : DataStoreFactory {

    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        @Suppress("UNCHECKED_CAST")
        return GoogleCredentialJPADataStore(id, userRepository, this) as DataStore<V>
    }
}