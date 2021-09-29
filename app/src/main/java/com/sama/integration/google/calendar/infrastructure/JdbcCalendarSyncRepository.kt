package com.sama.integration.google.calendar.infrastructure

import com.sama.common.to
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.infrastructure.toGoogleAccountId
import com.sama.integration.google.calendar.domain.CalendarSync
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime.ofInstant
import java.time.ZoneOffset.UTC
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component

@Component
class JdbcCalendarSyncRepository(private val jdbcTemplate: NamedParameterJdbcOperations) : CalendarSyncRepository {
    override fun find(accountId: GoogleAccountId, calendarId: GoogleCalendarId): CalendarSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", accountId.id)
            .addValue("calendar_id", calendarId)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_sync cs 
                   WHERE cs.google_account_id = :account_id AND cs.calendar_id = :calendar_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAll(accountId: GoogleAccountId): Collection<CalendarSync> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", accountId.id)

        return jdbcTemplate.query(
            """
               SELECT * FROM gcal.calendar_sync cs 
               WHERE cs.google_account_id = :account_id
            """,
            namedParameters,
            rowMapper
        )
    }

    override fun findAll(accountIds: Set<GoogleAccountId>): Collection<CalendarSync> {
        if (accountIds.isEmpty()) {
            return emptyList()
        }

        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_ids", accountIds.map { it.id })

        return jdbcTemplate.query(
            """
               SELECT * FROM gcal.calendar_sync cs 
               WHERE cs.google_account_id in (:account_ids)
            """,
            namedParameters,
            rowMapper
        )
    }

    override fun findAllCalendarIds(accountId: GoogleAccountId): Collection<GoogleCalendarId> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", accountId.id)

        return jdbcTemplate.queryForList(
            """
                       SELECT calendar_id FROM gcal.calendar_sync cs 
                       WHERE cs.google_account_id = :account_id
                    """,
            namedParameters,
            GoogleCalendarId::class.java
        )
    }

    override fun findAndLock(accountId: GoogleAccountId, calendarId: GoogleCalendarId): CalendarSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", accountId.id)
            .addValue("calendar_id", calendarId)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_sync cs 
                   WHERE cs.google_account_id = :account_id AND cs.calendar_id = :calendar_id
                   FOR UPDATE NOWAIT
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findSyncable(from: Instant): Collection<Pair<GoogleAccountId, GoogleCalendarId>> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("timestamp", ofInstant(from, UTC))

        return jdbcTemplate.query(
            """
               SELECT google_account_id, calendar_id FROM gcal.calendar_sync cs 
               WHERE cs.next_sync_at < :timestamp
            """,
            namedParameters
        ) { rs, _ -> rs.getLong("google_account_id").toGoogleAccountId() to rs.getString("calendar_id") }
    }

    override fun save(calendarSync: CalendarSync) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_sync (google_account_id, calendar_id, next_sync_at, failed_sync_count, sync_token, synced_from, synced_to, last_synced)  
                VALUES (:account_id, :calendar_id, :next_sync_at, :failed_sync_count, :sync_token, 
                        :synced_from, :synced_to, :last_synced)
                ON CONFLICT (google_account_id, calendar_id) DO UPDATE 
                SET next_sync_at = :next_sync_at, failed_sync_count = :failed_sync_count, sync_token = :sync_token, 
                    synced_from = :synced_from, synced_to = :synced_to, last_synced = :last_synced
            """,
            MapSqlParameterSource()
                .addValue("account_id", calendarSync.accountId.id)
                .addValue("calendar_id", calendarSync.calendarId)
                .addValue("next_sync_at", ofInstant(calendarSync.nextSyncAt, UTC))
                .addValue("failed_sync_count", calendarSync.failedSyncCount)
                .addValue("sync_token", calendarSync.syncToken)
                .addValue("synced_from", calendarSync.syncedRange?.start)
                .addValue("synced_to", calendarSync.syncedRange?.end)
                .addValue("last_synced", calendarSync.lastSynced?.let { ofInstant(it, UTC) })
        )
    }

    override fun delete(accountId: GoogleAccountId, calendarId: GoogleCalendarId) {
        jdbcTemplate.update(
            """
                DELETE FROM gcal.calendar_sync cs
                WHERE cs.google_account_id = :account_id AND cs.calendar_id = :calendar_id
            """,
            MapSqlParameterSource()
                .addValue("account_id", accountId.id)
                .addValue("calendar_id", calendarId)
        )
    }

    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarSync = { rs, _ ->
        CalendarSync(
            rs.getLong("google_account_id").toGoogleAccountId(),
            rs.getString("calendar_id"),
            rs.getTimestamp("next_sync_at")!!.toInstant(),
            rs.getInt("failed_sync_count"),
            rs.getString("sync_token"),
            rs.getDate("synced_from")?.toLocalDate() to rs.getDate("synced_to")?.toLocalDate(),
            rs.getTimestamp("last_synced")?.toInstant()
        )
    }
}