package com.sama.calendar.domain

import java.time.ZonedDateTime

interface BlockRepository {
    fun findAll(userId: Long, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Block>
}