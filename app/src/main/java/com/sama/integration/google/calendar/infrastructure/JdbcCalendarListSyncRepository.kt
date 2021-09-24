package com.sama.integration.google.calendar.infrastructure

import com.sama.integration.google.calendar.domain.CalendarListSync
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component

@Component
class JdbcCalendarListSyncRepository(private val jdbcTemplate: NamedParameterJdbcOperations) : CalendarListSyncRepository {
    override fun find(userId: UserId): CalendarListSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list_sync cls 
                   WHERE cls.user_id = :user_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAndLock(userId: UserId): CalendarListSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list_sync cls 
                   WHERE cls.user_id = :user_id
                   FOR UPDATE NOWAIT
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findSyncable(from: Instant): Collection<UserId> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("timestamp", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))

        return jdbcTemplate.query(
            """
               SELECT user_id FROM gcal.calendar_list_sync cls
               WHERE cls.next_sync_at < :timestamp
            """,
            namedParameters
        ) { rs, _ -> rs.getLong("user_id").toUserId() }
    }

    override fun save(calendarListSync: CalendarListSync) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_list_sync (user_id, google_account_id, next_sync_at, failed_sync_count, sync_token, last_synced)  
                VALUES (:user_id, :user_id, :next_sync_at, :failed_sync_count, :sync_token, :last_synced)
                ON CONFLICT (user_id, google_account_id) DO UPDATE 
                SET next_sync_at = :next_sync_at, failed_sync_count = :failed_sync_count, sync_token = :sync_token, last_synced = :last_synced
            """,
            MapSqlParameterSource()
                .addValue("user_id", calendarListSync.userId.id)
                .addValue("next_sync_at", OffsetDateTime.ofInstant(calendarListSync.nextSyncAt, ZoneOffset.UTC))
                .addValue("failed_sync_count", calendarListSync.failedSyncCount)
                .addValue("sync_token", calendarListSync.syncToken)
                .addValue("last_synced", calendarListSync.lastSynced?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) })
        )
    }

    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarListSync = { rs, _ ->
        CalendarListSync(
            rs.getLong("user_id").toUserId(),
            rs.getTimestamp("next_sync_at")!!.toInstant(),
            rs.getInt("failed_sync_count"),
            rs.getString("sync_token"),
            rs.getTimestamp("last_synced")?.toInstant()
        )
    }
}