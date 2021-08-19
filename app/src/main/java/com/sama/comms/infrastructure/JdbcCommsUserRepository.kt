package com.sama.comms.infrastructure

import com.sama.common.NotFoundException
import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.CommsUserRepository
import com.sama.users.domain.UserId
import java.sql.Types.BIGINT
import java.time.ZoneId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource

@Deprecated("Use alternative implementations")
class JdbcCommsUserRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) :
    CommsUserRepository {
    override fun findById(userId: UserId): CommsUser {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id, BIGINT)

        return namedParameterJdbcTemplate.queryForObject(
            """
                SELECT timezone, email FROM sama.user u 
                JOIN sama.user_settings us ON u.id = us.user_id  
                WHERE u.id = :user_id
            """.trimMargin(),
            namedParameters
        )
        { rs, _ ->
            CommsUser(userId, ZoneId.of(rs.getString("timezone")), rs.getString("email"))
        } ?: throw NotFoundException(CommsUser::class, userId)
    }
}