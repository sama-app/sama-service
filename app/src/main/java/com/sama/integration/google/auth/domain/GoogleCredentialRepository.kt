package com.sama.integration.google.auth.domain

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore
import java.time.Instant

interface GoogleCredentialRepository : DataStore<StoredCredential> {
    fun findInvalidatedIds(): Collection<Long>
}