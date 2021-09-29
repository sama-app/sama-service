package com.sama.integration.google.auth.infrastructure

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.AbstractDataStore
import com.google.api.client.util.store.DataStore
import com.sama.integration.google.GoogleInvalidCredentialsException
import java.sql.ResultSet
import org.apache.commons.logging.LogFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.crypto.encrypt.TextEncryptor

class EncryptedGoogleCredentialDataStore internal constructor(
    private val tokenEncryptor: TextEncryptor,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    dataStoreId: String,
    dataStoreFactory: GoogleCredentialDataStoreFactory,
) :
    AbstractDataStore<StoredCredential>(dataStoreFactory, dataStoreId),
    DataStore<StoredCredential> {
    private val logger = LogFactory.getLog(EncryptedGoogleCredentialDataStore::class.java)

    override fun get(id: String): StoredCredential? {
        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM sama.user_google_credential 
                   WHERE google_account_id = :google_account_id
                """,
                MapSqlParameterSource()
                    .addValue("google_account_id", id.toLong()),
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    private val rowMapper: (ResultSet, rowNum: Int) -> StoredCredential = { rs, _ ->
        StoredCredential().apply {
            accessToken = rs.getString("google_access_token_encrypted")
                ?.let { tokenEncryptor.decrypt(it) }
            refreshToken = rs.getString("google_refresh_token_encrypted")
                ?.let { tokenEncryptor.decrypt(it) }
            expirationTimeMilliseconds = rs.getLong("google_token_expiration_time_ms")
        }
    }

    override fun set(id: String, c: StoredCredential): DataStore<StoredCredential> {
        val parameterSource = MapSqlParameterSource()
            .addValue("google_account_id", id.toLong())
            .addValue("google_access_token_encrypted", c.accessToken?.let { tokenEncryptor.encrypt(it) })
            .addValue("google_token_expiration_time_ms", c.expirationTimeMilliseconds)

        jdbcTemplate.update(
            """
               INSERT INTO sama.user_google_credential (
                    google_account_id, 
                    google_access_token_encrypted, 
                    google_token_expiration_time_ms, 
                    updated_at
                )
               VALUES (
                    :google_account_id, 
                    :google_access_token_encrypted, 
                    :google_token_expiration_time_ms, 
                    CURRENT_TIMESTAMP
               )
               ON CONFLICT (google_account_id) DO UPDATE
               SET google_access_token_encrypted = :google_access_token_encrypted,
                   google_token_expiration_time_ms = :google_token_expiration_time_ms,
                   updated_at = CURRENT_TIMESTAMP
            """,
            parameterSource
        )

        if (c.refreshToken != null) {
            jdbcTemplate.update(
                """
               UPDATE sama.user_google_credential 
               SET google_refresh_token_encrypted = :google_refresh_token_encrypted
               WHERE google_account_id = :google_account_id
            """,
                MapSqlParameterSource()
                    .addValue("google_account_id", id.toLong())
                    .addValue("google_refresh_token_encrypted", tokenEncryptor.encrypt(c.refreshToken))
            )
        }

        // throw only after storing the invalid credentials so that Google's SDK
        // can handle the missing tokens itself
        if (!c.isUsable()) {
            logger.warn("User#$id received invalid Google Credentials")
            throw GoogleInvalidCredentialsException(null)
        }
        return this
    }

    override fun delete(id: String): DataStore<StoredCredential> {
        jdbcTemplate.update(
            """
               DELETE sama.user_google_credential 
               WHERE google_account_id = :google_account_id
            """,
            MapSqlParameterSource()
                .addValue("google_account_id", id.toLong())
        )
        return this
    }

    private fun StoredCredential.isUsable(): Boolean {
        // access token sent as null when credentials are invalidated or permissions
        // are removed
        return accessToken != null
    }

    override fun keySet(): Set<String> {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun values(): Collection<StoredCredential> {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun clear(): DataStore<StoredCredential> {
        throw UnsupportedOperationException("Unsupported")
    }
}