package com.sama.integration.google.auth.infrastructure

import com.sama.common.NotFoundException
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component


@Component
class JdbcGoogleAccountRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) : GoogleAccountRepository {

    private val rowMapper: (ResultSet, rowNum: Int) -> GoogleAccount = { rs, _ ->
        GoogleAccount(
            rs.getLong("id").toGoogleAccountId(),
            rs.getObject("public_id", UUID::class.java).toGoogleAccountPublicId(),
            rs.getLong("user_id").toUserId(),
            rs.getString("email"),
            rs.getBoolean("primary"),
            rs.getBoolean("linked"),
        )
    }

    override fun findByPublicIdOrThrow(googleAccountId: GoogleAccountPublicId): GoogleAccount {
        return try {
            jdbcTemplate.queryForObject(
                """
                    SELECT * FROM sama.user_google_account 
                    WHERE public_id = :public_id
                """,
                MapSqlParameterSource()
                    .addValue("public_id", googleAccountId.id),
                rowMapper
            )!!
        } catch (e: EmptyResultDataAccessException) {
            throw NotFoundException(GoogleAccount::class, googleAccountId.id)
        }
    }

    override fun findAllByUserId(userId: UserId): Collection<GoogleAccount> {
        return jdbcTemplate.query(
            """
                    SELECT * FROM sama.user_google_account 
                    WHERE user_id = :user_id
            """,
            MapSqlParameterSource()
                .addValue("user_id", userId.id),
            rowMapper
        )
    }

    override fun findByUserIdAndPrimary(userId: UserId): GoogleAccountId? {
        return jdbcTemplate.queryForObject(
            """
                SELECT id FROM sama.user_google_account 
                WHERE user_id = :user_id AND "primary" = :primary
            """,
            MapSqlParameterSource()
                .addValue("user_id", userId.id)
                .addValue("primary", true),
        ) { rs, _ -> rs.getLong("id").toGoogleAccountId() }
    }

    override fun save(googleAccount: GoogleAccount): GoogleAccount {
        val (id, publicId) = if (googleAccount.id == null) {
            val keyHolder: KeyHolder = GeneratedKeyHolder()
            jdbcTemplate.update(
                """
                    INSERT INTO sama.user_google_account (user_id, email, "primary", linked, created_at, updated_at) 
                    VALUES (:user_id, :email, :primary, :linked, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    RETURNING id, public_id
                """,
                MapSqlParameterSource()
                    .addValue("user_id", googleAccount.userId.id)
                    .addValue("email", googleAccount.email)
                    .addValue("primary", googleAccount.primary)
                    .addValue("linked", googleAccount.linked),
                keyHolder
            )
            keyHolder.keys["id"] as Long to keyHolder.keys["public_id"] as UUID
        } else {
            jdbcTemplate.update(
                """
                    UPDATE sama.user_google_account 
                    SET "primary" = :primary, linked = :linked, updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                """,
                MapSqlParameterSource()
                    .addValue("id", googleAccount.id.id)
                    .addValue("primary", googleAccount.primary)
                    .addValue("linked", googleAccount.linked)
            )
            googleAccount.id.id to googleAccount.publicId!!.id
        }

        return googleAccount.copy(id = id.toGoogleAccountId(), publicId = publicId.toGoogleAccountPublicId())
    }
}

fun Long.toGoogleAccountId() = GoogleAccountId(this)
fun UUID.toGoogleAccountPublicId() = GoogleAccountPublicId(this)