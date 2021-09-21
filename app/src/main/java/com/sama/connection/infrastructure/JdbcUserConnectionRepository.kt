package com.sama.connection.infrastructure

import com.sama.connection.domain.UserConnection
import com.sama.connection.domain.UserConnectionRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
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
            .addValue("user_id", userId.id, Types.BIGINT)

        return namedParameterJdbcTemplate.query(
            """
                SELECT uc.connected_user_id FROM (
                    SELECT l_user_id AS connected_user_id FROM sama.user_connection 
                    WHERE r_user_id = :user_id
                    UNION
                    SELECT r_user_id AS connected_user_id  FROM sama.user_connection
                    WHERE l_user_id = :user_id
                ) uc
            """,
            namedParameters
        ) { rs, _ -> rs.getLong("connected_user_id").toUserId() }
    }

    override fun exists(userOneId: UserId, userTwoId: UserId): Boolean {
        // always put the smaller id in the first column to ensure uniqueness
        val namedParameters = MapSqlParameterSource()
            .addValue("l_user_id", min(userOneId.id, userTwoId.id))
            .addValue("r_user_id", max(userOneId.id, userTwoId.id))

        return namedParameterJdbcTemplate.queryForObject(
            """
                SELECT EXISTS(SELECT 1 FROM sama.user_connection WHERE l_user_id = :l_user_id AND r_user_id = :r_user_id)
            """,
            namedParameters,
            Boolean::class.java
        )
    }

    override fun save(userConnection: UserConnection) {
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO sama.user_connection (l_user_id, r_user_id, created_at)  
                VALUES (:l_user_id, :r_user_id, CURRENT_TIMESTAMP)
            """,
            userConnection.toSqlParameterSource()
        )
    }

    override fun delete(userConnection: UserConnection) {
        namedParameterJdbcTemplate.update(
            """
                DELETE FROM sama.user_connection
                WHERE l_user_id = :l_user_id AND r_user_id = :r_user_id
            """,
            userConnection.toSqlParameterSource()
        )
    }

    fun UserConnection.toSqlParameterSource(): SqlParameterSource {
        // always put the smaller id in the first column to ensure uniqueness
        return MapSqlParameterSource()
            .addValue("l_user_id", min(leftUserId.id, rightUserId.id))
            .addValue("r_user_id", max(leftUserId.id, rightUserId.id))
    }
}