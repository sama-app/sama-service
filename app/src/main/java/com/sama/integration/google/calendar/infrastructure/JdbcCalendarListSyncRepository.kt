package com.sama.integration.google.calendar.infrastructure

import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.infrastructure.toGoogleAccountId
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

    override fun find(googleAccountId: GoogleAccountId): CalendarListSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", googleAccountId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list_sync cls 
                   WHERE cls.google_account_id = :account_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAndLock(googleAccountId: GoogleAccountId): CalendarListSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", googleAccountId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list_sync cls 
                   WHERE cls.google_account_id = :account_id
                   FOR UPDATE NOWAIT
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findSyncable(from: Instant): Collection<GoogleAccountId> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("timestamp", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))

        return jdbcTemplate.query(
            """
               SELECT google_account_id FROM gcal.calendar_list_sync cls
               WHERE cls.next_sync_at < :timestamp
            """,
            namedParameters
        ) { rs, _ -> rs.getLong("google_account_id").toGoogleAccountId() }
    }

    override fun save(calendarListSync: CalendarListSync) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_list_sync (google_account_id, next_sync_at, failed_sync_count, sync_token, last_synced)  
                VALUES (:account_id, :next_sync_at, :failed_sync_count, :sync_token, :last_synced)
                ON CONFLICT (google_account_id, google_account_id) DO UPDATE 
                SET next_sync_at = :next_sync_at, failed_sync_count = :failed_sync_count, 
                    sync_token = :sync_token, last_synced = :last_synced
            """,
            MapSqlParameterSource()
                .addValue("account_id", calendarListSync.accountId.id)
                .addValue("next_sync_at", OffsetDateTime.ofInstant(calendarListSync.nextSyncAt, ZoneOffset.UTC))
                .addValue("failed_sync_count", calendarListSync.failedSyncCount)
                .addValue("sync_token", calendarListSync.syncToken)
                .addValue("last_synced", calendarListSync.lastSynced?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) })
        )
    }

    override fun deleteBy(accountId: GoogleAccountId) {
        jdbcTemplate.update(
            """
                DELETE FROM gcal.calendar_list_sync cs
                WHERE cs.google_account_id = :account_id
            """,
            MapSqlParameterSource()
                .addValue("account_id", accountId.id)
        )
    }

    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarListSync = { rs, _ ->
        CalendarListSync(
            rs.getLong("google_account_id").toGoogleAccountId(),
            rs.getTimestamp("next_sync_at")!!.toInstant(),
            rs.getInt("failed_sync_count"),
            rs.getString("sync_token"),
            rs.getTimestamp("last_synced")?.toInstant()
        )
    }
}