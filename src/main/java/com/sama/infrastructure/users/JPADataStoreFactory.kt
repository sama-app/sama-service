package com.sama.infrastructure.users

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class JPADataStoreFactory(private val userRepository: UserRepository) : DataStoreFactory {

    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        @Suppress("UNCHECKED_CAST")
        return JPADataStore(id, userRepository, this) as DataStore<V>
    }
}