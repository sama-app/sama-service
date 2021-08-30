package com.sama.integration.google.calendar.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
import java.sql.ResultSet
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component


@Component
class JdbcCalendarEventRepository(
    private val jdbcTemplate: NamedParameterJdbcOperations,
    private val objectMapper: ObjectMapper,
) : CalendarEventRepository {
    private val UTC = ZoneId.of("UTC")

    override fun find(id: GoogleCalendarEventId): CalendarEvent? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("id", id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.event e 
                   WHERE e.id = :id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAll(
        userId: UserId,
        calendarId: GoogleCalendarId,
        from: ZonedDateTime,
        to: ZonedDateTime,
    ): List<CalendarEvent> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)
            .addValue("calendar_id", calendarId)
            .addValue("from", from.withZoneSameInstant(UTC).toLocalDateTime())
            .addValue("to", to.withZoneSameInstant(UTC).toLocalDateTime())

        return jdbcTemplate.query(
            """
                SELECT * FROM gcal.event e 
                WHERE e.user_id = :user_id AND e.calendar_id = :calendar_id AND e.start_date_time < :to AND e.end_date_time >= :from
            """,
            namedParameters,
            rowMapper
        )
    }

    override fun save(event: CalendarEvent) {
        saveAll(listOf(event))
    }

    override fun saveAll(events: Collection<CalendarEvent>) {
        if (events.isEmpty()) {
            return
        }
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO gcal.event (id, user_id, calendar_id, start_date_time, end_date_time, event_data, updated_at)
                VALUES (:id, :user_id, :calendar_id, :start_date_time, :end_date_time, :event_data, :updated_at)
                ON CONFLICT (id) DO UPDATE 
                SET start_date_time = :start_date_time, end_date_time = :end_date_time, event_data = :event_data, updated_at = :updated_at
            """,
            events.map { event ->
                MapSqlParameterSource()
                    .addValue("id", event.googleEventId)
                    .addValue("user_id", event.userId.id)
                    .addValue("calendar_id", event.calendarId)
                    .addValue("start_date_time", event.startDateTime.withZoneSameInstant(UTC).toLocalDateTime())
                    .addValue("end_date_time", event.endDateTime.withZoneSameInstant(UTC).toLocalDateTime())
                    .addValue("event_data", objectMapper.writeValueAsString(event.eventData), Types.OTHER)
                    .addValue("updated_at", OffsetDateTime.now(ZoneId.of("UTC")))
            }.toTypedArray()
        )
    }

    override fun deleteAll(eventIds: Collection<GoogleCalendarEventId>) {
        if (eventIds.isEmpty()) {
            return
        }
        jdbcTemplate.update(
            """
                DELETE FROM gcal.event WHERE id in (:event_ids)
            """,
            MapSqlParameterSource().addValue("event_ids", eventIds)
        )
    }


    override fun deleteBy(userId: UserId, calendarId: GoogleCalendarId) {
        jdbcTemplate.update(
            """
                DELETE FROM gcal.event WHERE user_id = :user_id AND calendar_id = :calendar_id
            """,
            MapSqlParameterSource()
                .addValue("user_id", userId.id)
                .addValue("calendar_id", calendarId)
        )
    }


    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarEvent = { rs, _ ->
        CalendarEvent(
            rs.getLong("user_id").toUserId(),
            rs.getString("id"),
            rs.getString("calendar_id"),
            rs.getTimestamp("start_date_time").toLocalDateTime().atZone(UTC),
            rs.getTimestamp("end_date_time").toLocalDateTime().atZone(UTC),
            objectMapper.readValue(rs.getBinaryStream("event_data"), EventData::class.java)
        )
    }
}