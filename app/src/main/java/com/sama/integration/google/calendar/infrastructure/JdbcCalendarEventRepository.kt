package com.sama.integration.google.calendar.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.infrastructure.toGoogleAccountId
import com.sama.integration.google.calendar.domain.CalendarEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.EventData
import com.sama.integration.google.calendar.domain.EventLabel
import com.sama.integration.google.calendar.domain.GoogleCalendarEventKey
import com.sama.integration.google.calendar.domain.GoogleCalendarId
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

    override fun find(eventKey: GoogleCalendarEventKey): CalendarEvent? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("google_account_id", eventKey.accountId.id)
            .addValue("calendar_id", eventKey.calendarId)
            .addValue("event_id", eventKey.eventId)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.event e 
                   WHERE e.google_account_id = :google_account_id AND e.calendar_id = :calendar_id and e.event_id = :event_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAll(eventKeys: Collection<GoogleCalendarEventKey>): List<CalendarEvent> {
        if (eventKeys.isEmpty()) {
            return emptyList()
        }

        val params = eventKeys.map {
            arrayOf(it.accountId.id, it.calendarId, it.eventId)
        }

        return jdbcTemplate.query(
            """
                   SELECT * FROM gcal.event e 
                   WHERE (e.google_account_id, e.calendar_id, e.event_id) IN (:event_keys)
                """,
            MapSqlParameterSource()
                .addValue("event_keys", params),
            rowMapper
        )
    }

    override fun findAll(
        accountId: GoogleAccountId,
        calendarId: GoogleCalendarId,
        from: ZonedDateTime,
        to: ZonedDateTime
    ): List<CalendarEvent> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("google_account_id", accountId.id)
            .addValue("calendar_id", calendarId)
            .addValue("from", from.withZoneSameInstant(UTC).toLocalDateTime())
            .addValue("to", to.withZoneSameInstant(UTC).toLocalDateTime())

        return jdbcTemplate.query(
            """
                SELECT * FROM gcal.event e 
                WHERE e.google_account_id = :google_account_id 
                    AND e.calendar_id = :calendar_id 
                    AND e.start_date_time < :to 
                    AND e.end_date_time >= :from
            """,
            namedParameters,
            rowMapper
        )
    }

    override fun findAll(
        accountId: GoogleAccountId,
        calendarId: GoogleCalendarId,
        from: ZonedDateTime,
        to: ZonedDateTime,
        createdFrom: ZonedDateTime?,
        minAttendeeCount: Int?,
    ): List<CalendarEvent> {
        val namedParameters = MapSqlParameterSource()
            .addValue("google_account_id", accountId.id)
            .addValue("calendar_id", calendarId)
            .addValue("from", from.withZoneSameInstant(UTC).toLocalDateTime())
            .addValue("to", to.withZoneSameInstant(UTC).toLocalDateTime())

        var query = """
                SELECT * FROM gcal.event e 
                WHERE e.google_account_id = :google_account_id 
                    AND e.calendar_id = :calendar_id 
                    AND e.start_date_time < :to 
                    AND e.end_date_time >= :from 
            """

        createdFrom?.let {
            namedParameters.addValue("created_from", it.withZoneSameInstant(UTC).toLocalDateTime())
            query += """
                AND ((e.event_data ->> 'created')::timestamp without time zone >= :created_from OR 
                    (e.event_data ->> 'created') IS NULL)
            """
        }

        minAttendeeCount?.let {
            namedParameters.addValue("min_attendee_count", minAttendeeCount)
            query += """
                AND (e.event_data ->> 'attendeeCount')::int >= :min_attendee_count
            """
        }

        return jdbcTemplate.query(query, namedParameters, rowMapper)
    }

    override fun save(event: CalendarEvent) {
        saveAll(listOf(event))
    }

    override fun saveAll(events: Collection<CalendarEvent>) {
        if (events.isEmpty()) {
            return
        }
        events.chunked(250) {
            it.map { (key, startDateTime, endDateTime, eventData, labels) ->
                MapSqlParameterSource()
                    .addValue("google_account_id", key.accountId.id)
                    .addValue("calendar_id", key.calendarId)
                    .addValue("event_id", key.eventId)
                    .addValue("start_date_time", startDateTime.withZoneSameInstant(UTC).toLocalDateTime())
                    .addValue("end_date_time", endDateTime.withZoneSameInstant(UTC).toLocalDateTime())
                    .addValue("event_data", objectMapper.writeValueAsString(eventData), Types.OTHER)
                    .addValue("labels", objectMapper.writeValueAsString(labels), Types.OTHER)
                    .addValue("updated_at", OffsetDateTime.now(ZoneId.of("UTC")))
            }.toTypedArray()
        }.forEach { params ->
            jdbcTemplate.batchUpdate(
                """
                    INSERT INTO gcal.event (google_account_id, calendar_id, event_id, start_date_time, end_date_time, event_data, labels, updated_at)
                    VALUES (:google_account_id, :calendar_id, :event_id, :start_date_time, :end_date_time, :event_data, :labels, :updated_at)
                    ON CONFLICT (google_account_id, calendar_id, event_id) DO UPDATE 
                    SET start_date_time = :start_date_time, end_date_time = :end_date_time, 
                        event_data = :event_data, labels = :labels, updated_at = :updated_at
                    """,
                params
            )
        }
    }

    override fun deleteAll(eventKeys: Collection<GoogleCalendarEventKey>) {
        if (eventKeys.isEmpty()) {
            return
        }
        eventKeys
            .chunked(250)
            .forEach { chunk ->
                jdbcTemplate.update(
                    """
                    DELETE FROM gcal.event WHERE (google_account_id, calendar_id, event_id) in (:event_keys)
                    """,
                    MapSqlParameterSource().addValue("event_keys", chunk
                        .map { arrayOf(it.accountId.id, it.calendarId, it.eventId) })
                )
            }
    }

    override fun deleteBy(accountId: GoogleAccountId, calendarId: GoogleCalendarId) {
        jdbcTemplate.update(
            """
                DELETE FROM gcal.event WHERE google_account_id = :google_account_id AND calendar_id = :calendar_id
            """,
            MapSqlParameterSource()
                .addValue("google_account_id", accountId.id)
                .addValue("calendar_id", calendarId)
        )
    }


    private val listTypeRef: TypeReference<Set<EventLabel>> = object : TypeReference<Set<EventLabel>>() {}

    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarEvent = { rs, _ ->
        CalendarEvent(
            GoogleCalendarEventKey(
                rs.getLong("google_account_id").toGoogleAccountId(),
                rs.getString("calendar_id"),
                rs.getString("event_id")
            ),
            rs.getTimestamp("start_date_time").toLocalDateTime().atZone(UTC),
            rs.getTimestamp("end_date_time").toLocalDateTime().atZone(UTC),
            objectMapper.readValue(rs.getBinaryStream("event_data"), EventData::class.java),
            objectMapper.readValue(rs.getBinaryStream("labels"), listTypeRef)
        )
    }
}