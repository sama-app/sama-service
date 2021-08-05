package com.sama.connection.infrastructure

import com.sama.connection.domain.UserConnection
import com.sama.connection.domain.UserConnectionRepository
import com.sama.users.domain.UserId
import java.lang.Long.max
import java.lang.Long.min
import java.sql.Types
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository

@Repository
class JdbcUserConnectionRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) :
    UserConnectionRepository {

    override fun findConnectedUserIds(userId: UserId): Collection<UserId> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId, Types.BIGINT)

        return namedParameterJdbcTemplate.query(
            """
                SELECT uc.connected_user_id FROM (
                    SELECT l_user_id AS connected_user_id FROM sama.user_connection 
                    WHERE r_user_id = :user_id
                    UNION
                    SELECT r_user_id AS connected_user_id  FROM sama.user_connection
                    WHERE l_user_id = :user_id
                ) uc
            """.trimIndent(),
            namedParameters
        ) { rs, _ -> rs.getLong("connected_user_id") }
    }

    override fun save(userConnection: UserConnection) {
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO sama.user_connection (l_user_id, r_user_id, created_at)  
                VALUES (:l_user_id, :r_user_id, CURRENT_TIMESTAMP)
            """.trimIndent(),
            userConnection.toSqlParameterSource()
        )
    }

    override fun delete(userConnection: UserConnection) {
        namedParameterJdbcTemplate.update(
            """
                DELETE FROM sama.user_connection
                WHERE l_user_id = :l_user_id AND r_user_id = :r_user_id
            """.trimIndent(),
            userConnection.toSqlParameterSource()
        )
    }

    fun UserConnection.toSqlParameterSource(): SqlParameterSource {
        // always put the smaller id in the first column to ensure uniqueness
        return MapSqlParameterSource()
            .addValue("l_user_id", min(this.leftUserId, this.rightUserId))
            .addValue("r_user_id", max(this.leftUserId, this.rightUserId))
    }
}