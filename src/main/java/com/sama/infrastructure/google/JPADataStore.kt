package com.sama.infrastructure.google

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.AbstractDataStore
import com.google.api.client.util.store.DataStore
import com.sama.auth.domain.AuthUser
import com.sama.auth.domain.AuthUserRepository
import com.sama.auth.domain.GoogleCredential
import com.sama.common.toNullable
import liquibase.pro.packaged.it
import java.util.function.Function
import java.util.stream.Collectors

class JPADataStore internal constructor(
    dataStoreId: String,
    private val authUserRepository: AuthUserRepository,
    dataStoreFactory: JPADataStoreFactory
) : AbstractDataStore<StoredCredential>(dataStoreFactory, dataStoreId), DataStore<StoredCredential> {

    override fun keySet(): Set<String> {
        return authUserRepository.findAllIds()
            .map { it.toString() }
            .toSet()
    }

    override fun values(): Collection<StoredCredential> {
        return authUserRepository.findAll().stream()
            .map(toStoredCredential)
            .collect(Collectors.toList())
    }

    override fun get(id: String): StoredCredential? {
        return authUserRepository.findById(id.toLong()).toNullable()
            ?.googleCredential()
            ?.let {
                val credential = StoredCredential()
                credential.accessToken = it.accessToken
                credential.refreshToken = it.refreshToken
                credential.expirationTimeMilliseconds = it.expirationTimeMs
                credential
            }
    }

    override fun set(id: String, credential: StoredCredential): DataStore<StoredCredential> {
        authUserRepository.findById(id.toLong()).toNullable()
            ?.let {
                when {
                    credential.refreshToken != null -> {
                        it.initGoogleCredential(
                            credential.accessToken,
                            credential.refreshToken,
                            credential.expirationTimeMilliseconds
                        )
                    }
                    else -> {
                        it.refreshGoogleCredential(
                            credential.accessToken,
                            credential.expirationTimeMilliseconds
                        )
                    }
                }
                it
            }
            ?.also { authUserRepository.save(it) }

        return this
    }

    override fun clear(): DataStore<StoredCredential> {
        authUserRepository.deleteAll() // todo remove only google credentials
        return this
    }

    override fun delete(id: String): DataStore<StoredCredential> {
        authUserRepository.findById(id.toLong()).toNullable()
            ?.removeGoogleCredential()
            ?.also { authUserRepository.save(it) }
        return this
    }

    private val toStoredCredential = Function { au: AuthUser ->
        val credential = StoredCredential()
        val gc: GoogleCredential = au.googleCredential()!!
        credential.accessToken = gc.accessToken
        credential.refreshToken = gc.refreshToken
        credential.expirationTimeMilliseconds = gc.expirationTimeMs
        credential
    }
}