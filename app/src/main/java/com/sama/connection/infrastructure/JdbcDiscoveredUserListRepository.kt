package com.sama.connection.infrastructure

import com.sama.connection.domain.DiscoveredUserList
import com.sama.connection.domain.DiscoveredUserListRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.Types
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository


@Repository
class JdbcDiscoveredUserListRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) :
    DiscoveredUserListRepository {

    override fun findById(userId: UserId): DiscoveredUserList {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id, Types.BIGINT)

        val discoveredUserIds = namedParameterJdbcTemplate.query(
            """
                SELECT discovered_user_id FROM sama.user_discovered_user udu 
                WHERE udu.user_id = :user_id
            """.trimIndent(),
            namedParameters
        ) { rs, _ -> rs.getLong("discovered_user_id").toUserId() }

        return DiscoveredUserList(userId, discoveredUserIds.toSet())
    }

    override fun save(discoveredUserList: DiscoveredUserList) {
        if (discoveredUserList.discoveredUsers.isNotEmpty()) {
            namedParameterJdbcTemplate.update(
                """
                    DELETE FROM sama.user_discovered_user udu
                    WHERE udu.user_id = :user_id AND udu.discovered_user_id NOT IN (:discovered_user_ids)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("user_id", discoveredUserList.userId.id, Types.BIGINT)
                    .addValue("discovered_user_ids", discoveredUserList.discoveredUsers.map { it.id })
            )

            namedParameterJdbcTemplate.batchUpdate(
                """
                    INSERT INTO sama.user_discovered_user (user_id, discovered_user_id)  
                    VALUES (:user_id, :discovered_user_id)
                    ON CONFLICT DO NOTHING
                """.trimIndent(),
                discoveredUserList.discoveredUsers
                    .map {
                        MapSqlParameterSource()
                            .addValue("user_id", discoveredUserList.userId.id, Types.BIGINT)
                            .addValue("discovered_user_id", it.id, Types.BIGINT)
                    }.toTypedArray()
            )
        } else {
            namedParameterJdbcTemplate.update(
                """
                    DELETE FROM sama.user_discovered_user udu
                    WHERE udu.user_id = :user_id
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("user_id", discoveredUserList.userId.id, Types.BIGINT)
            )
        }
    }
}