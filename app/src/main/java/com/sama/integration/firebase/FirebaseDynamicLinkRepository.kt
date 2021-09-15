package com.sama.integration.firebase

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository

@Repository
class FirebaseDynamicLinkRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) {

    fun find(key: String): String? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("key", key)

        return try {
            namedParameterJdbcTemplate.queryForObject(
                """
                   SELECT dynamic_link FROM sama.firebase_dynamic_link fdl 
                   WHERE fdl.key = :key
                """.trimIndent(),
                namedParameters,
                String::class.java
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    fun save(key: String, dynamicLink: String) {
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO sama.firebase_dynamic_link (key, dynamic_link)  
                VALUES (:key, :dynamic_link) ON CONFLICT DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("key", key)
                .addValue("dynamic_link", dynamicLink)
        )
    }
}