package com.sama.integration.google.calendar.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.infrastructure.toGoogleAccountId
import com.sama.integration.google.calendar.domain.Calendar
import com.sama.integration.google.calendar.domain.CalendarList
import com.sama.integration.google.calendar.domain.CalendarListRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import java.sql.ResultSet
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneId
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component

@Component
class JdbcCalendarListRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : CalendarListRepository {

    override fun find(accountId: GoogleAccountId): CalendarList? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_id", accountId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list e 
                   WHERE e.google_account_id = :account_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun findAll(accountIds: Collection<GoogleAccountId>): Collection<CalendarList> {
        if (accountIds.isEmpty()) {
            return emptyList()
        }
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("account_ids", accountIds.map { it.id })

        return jdbcTemplate.query(
            """
                   SELECT * FROM gcal.calendar_list e 
                   WHERE e.google_account_id in (:account_ids)
                """,
            namedParameters,
            rowMapper
        )
    }

    override fun save(calendarList: CalendarList) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_list (google_account_id, calendars, selected, updated_at)  
                VALUES (:account_id, :calendars, :selected, :updated_at)
                ON CONFLICT (google_account_id) DO UPDATE 
                SET calendars = :calendars, selected = :selected, updated_at = :updated_at
            """,
            MapSqlParameterSource()
                .addValue("account_id", calendarList.accountId.id)
                .addValue("calendars", objectMapper.writeValueAsString(calendarList.calendars), Types.OTHER)
                .addValue("selected", objectMapper.writeValueAsString(calendarList.selected), Types.OTHER)
                .addValue("updated_at", OffsetDateTime.now(ZoneId.of("UTC")))
        )
    }

    override fun delete(accountId: GoogleAccountId) {
        jdbcTemplate.update(
            """
                DELETE FROM gcal.calendar_list cl
                WHERE cl.google_account_id = :account_id
            """,
            MapSqlParameterSource()
                .addValue("account_id", accountId.id)
        )
    }


    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarList = { rs, _ ->
        val calendarsTypeRef: TypeReference<HashMap<GoogleCalendarId, Calendar>> =
            object : TypeReference<HashMap<GoogleCalendarId, Calendar>>() {}
        val selectedTypeRef: TypeReference<Set<GoogleCalendarId>> =
            object : TypeReference<Set<GoogleCalendarId>>() {}

        CalendarList(
            rs.getLong("google_account_id").toGoogleAccountId(),
            objectMapper.readValue(rs.getBinaryStream("calendars"), calendarsTypeRef),
            objectMapper.readValue(rs.getBinaryStream("selected"), selectedTypeRef),
        )
    }
}