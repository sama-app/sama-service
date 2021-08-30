package com.sama.integration.google.calendar.infrastructure

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
    override fun find(userId: UserId, calendarId: GoogleCalendarId): CalendarSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)
            .addValue("calendar_id", calendarId)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_sync cs 
                   WHERE cs.user_id = :user_id AND cs.calendar_id = :calendar_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAndLock(userId: UserId, calendarId: GoogleCalendarId): CalendarSync? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)
            .addValue("calendar_id", calendarId)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_sync cs 
                   WHERE cs.user_id = :user_id AND cs.calendar_id = :calendar_id
                   FOR UPDATE NOWAIT
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findSyncable(time: Instant): Collection<Pair<UserId, GoogleCalendarId>> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("timestamp", ofInstant(time, UTC))

        return jdbcTemplate.query(
            """
               SELECT user_id, calendar_id FROM gcal.calendar_sync cs 
               WHERE cs.next_sync_at < :timestamp
            """,
            namedParameters
        ) { rs, _ -> rs.getLong("user_id").toUserId() to rs.getString("calendar_id") }
    }

    override fun save(calendarSync: CalendarSync) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_sync (user_id, calendar_id, next_sync_at, failed_sync_count, sync_token, synced_from, synced_to, last_synced)  
                VALUES (:user_id, :calendar_id, :next_sync_at, :failed_sync_count, :sync_token, :synced_from, :synced_to, :last_synced)
                ON CONFLICT (user_id, calendar_id) DO UPDATE 
                SET next_sync_at = :next_sync_at, failed_sync_count = :failed_sync_count, sync_token = :sync_token, 
                    synced_from = :synced_from, synced_to = :synced_to, last_synced = :last_synced
            """,
            MapSqlParameterSource()
                .addValue("user_id", calendarSync.userId.id)
                .addValue("calendar_id", calendarSync.calendarId)
                .addValue("next_sync_at", ofInstant(calendarSync.nextSyncAt, UTC))
                .addValue("failed_sync_count", calendarSync.failedSyncCount)
                .addValue("sync_token", calendarSync.syncToken)
                .addValue("synced_from", calendarSync.syncedFrom)
                .addValue("synced_to", calendarSync.syncedTo)
                .addValue("last_synced", calendarSync.lastSynced?.let { ofInstant(it, UTC) })
        )
    }

    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarSync = { rs, _ ->
        CalendarSync(
            rs.getLong("user_id").toUserId(),
            rs.getString("calendar_id"),
            rs.getTimestamp("next_sync_at")!!.toInstant(),
            rs.getInt("failed_sync_count"),
            rs.getString("sync_token"),
            rs.getDate("synced_from")?.toLocalDate(),
            rs.getDate("synced_to")?.toLocalDate(),
            rs.getTimestamp("last_synced")?.toInstant()
        )
    }
}