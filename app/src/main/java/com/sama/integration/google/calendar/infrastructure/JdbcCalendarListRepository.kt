package com.sama.integration.google.calendar.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sama.integration.google.calendar.domain.Calendar
import com.sama.integration.google.calendar.domain.CalendarList
import com.sama.integration.google.calendar.domain.CalendarListRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.toUserId
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

    override fun find(userId: UserId): CalendarList? {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId.id)

        return try {
            jdbcTemplate.queryForObject(
                """
                   SELECT * FROM gcal.calendar_list e 
                   WHERE e.user_id = :user_id
                """,
                namedParameters,
                rowMapper
            )
        } catch (e: EmptyResultDataAccessException) {
            return null
        }
    }

    override fun save(calendarList: CalendarList) {
        jdbcTemplate.update(
            """
                INSERT INTO gcal.calendar_list (user_id, google_account_id, calendars, updated_at)  
                VALUES (:user_id, :user_id, :calendars, :updated_at)
                ON CONFLICT (user_id, google_account_id) DO UPDATE 
                SET calendars = :calendars, updated_at = :updated_at
            """,
            MapSqlParameterSource()
                .addValue("user_id", calendarList.userId.id)
                .addValue("calendars", objectMapper.writeValueAsString(calendarList.calendars), Types.OTHER)
                .addValue("updated_at", OffsetDateTime.now(ZoneId.of("UTC")))
        )
    }


    private val rowMapper: (ResultSet, rowNum: Int) -> CalendarList = { rs, _ ->
        val typeRef: TypeReference<HashMap<GoogleCalendarId, Calendar>> =
            object : TypeReference<HashMap<GoogleCalendarId, Calendar>>() {}

        CalendarList(
            rs.getLong("user_id").toUserId(),
            objectMapper.readValue(rs.getBinaryStream("calendars"), typeRef)
        )
    }
}