package com.sama.connection.infrastructure

import com.sama.common.NotFoundException
import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.connection.domain.ConnectionRequestStatus
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.ResultSet
import java.util.UUID
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository

@Repository
class JdbcConnectionRequestRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) :
    ConnectionRequestRepository {

    private val rowMapper: (ResultSet, rowNum: Int) -> ConnectionRequest = { rs, _ ->
        ConnectionRequest(
            rs.getObject("id", UUID::class.java),
            rs.getLong("initiator_user_id").toUserId(),
            rs.getLong("recipient_user_id").toUserId(),
            ConnectionRequestStatus.valueOf(rs.getString("status"))
        )
    }

    override fun findByIdOrThrow(connectionRequestId: ConnectionRequestId): ConnectionRequest {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("id", connectionRequestId)

        return try {
            namedParameterJdbcTemplate.queryForObject(
                """
                   SELECT * FROM sama.user_connection_request ucr 
                   WHERE ucr.id = :id
                """.trimIndent(),
                namedParameters,
                rowMapper
            )!!
        } catch (e: EmptyResultDataAccessException) {
            throw NotFoundException(ConnectionRequest::class, connectionRequestId)
        }
    }

    override fun findPendingByInitiatorId(userId: UserId): Collection<ConnectionRequest> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)
            .addValue("status", ConnectionRequestStatus.PENDING.name)

        return namedParameterJdbcTemplate.query(
            """
                SELECT * FROM sama.user_connection_request ucr 
                WHERE ucr.initiator_user_id = :user_id AND status = :status
            """.trimIndent(),
            namedParameters,
            rowMapper
        )
    }

    override fun findPendingByRecipientId(userId: UserId): Collection<ConnectionRequest> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)
            .addValue("status", ConnectionRequestStatus.PENDING.name)

        return namedParameterJdbcTemplate.query(
            """
                SELECT * FROM sama.user_connection_request ucr 
                WHERE ucr.recipient_user_id = :user_id AND status = :status
            """.trimIndent(),
            namedParameters,
            rowMapper
        )
    }

    override fun findPendingByUserIds(initiatorId: UserId, recipientId: UserId): ConnectionRequest? {
        return try {
            namedParameterJdbcTemplate.queryForObject(
                """
                    SELECT * FROM sama.user_connection_request ucr
                    WHERE ucr.status = :status AND ucr.initiator_user_id = :initiator_user_id
                    AND ucr.recipient_user_id = :recipient_user_id
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("status", ConnectionRequestStatus.PENDING.name)
                    .addValue("initiator_user_id", initiatorId.id)
                    .addValue("recipient_user_id", recipientId.id),
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun save(connectionRequest: ConnectionRequest) {
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO sama.user_connection_request (id, initiator_user_id, recipient_user_id, status)  
                VALUES (:id, :initiator_user_id, :recipient_user_id, :status)
                ON CONFLICT (id) DO UPDATE 
                SET status = :status, updated_at = CURRENT_TIMESTAMP
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", connectionRequest.id)
                .addValue("initiator_user_id", connectionRequest.initiatorUserId.id)
                .addValue("recipient_user_id", connectionRequest.recipientUserId.id)
                .addValue("status", connectionRequest.status.name)
        )
    }
}