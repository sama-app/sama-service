package com.sama.slotsuggestion.infrastructure

import com.sama.common.NotFoundException
import com.sama.slotsuggestion.domain.User
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.WorkingHours
import com.sama.users.domain.UserId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository
import java.sql.Types.BIGINT
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

@Repository
class JdbcUserRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcOperations) : UserRepository {
    override fun findById(userId: UserId): User {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
            .addValue("user_id", userId, BIGINT)

        val zoneId = namedParameterJdbcTemplate.queryForObject(
            """SELECT timezone FROM sama.user_settings WHERE user_id = :user_id""",
            namedParameters,
            ZoneId::class.java
        ) ?: throw NotFoundException(User::class, userId)

        val workingHours: MutableMap<DayOfWeek, WorkingHours> = mutableMapOf()
        namedParameterJdbcTemplate.query(
            """
                SELECT day_of_week, start_time, end_time 
                FROM sama.user_day_working_hours 
                WHERE user_id = :user_id
           """,
            namedParameters
        ) { rs, _ ->
            val dayOfWeek = DayOfWeek.valueOf(rs.getString("day_of_week"))
            workingHours[dayOfWeek] = WorkingHours(
                rs.getObject("start_time", LocalTime::class.java),
                rs.getObject("end_time", LocalTime::class.java)
            )
        } ?: throw NotFoundException(User::class, userId)


        return User(userId, zoneId, workingHours)
    }
}