package com.sama.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.streams.asSequence
import org.threeten.extra.LocalDateRange


fun Long.toMinutes(): Duration = Duration.ofMinutes(this)

fun LocalDate.datesUtil(endDate: LocalDate): Sequence<LocalDate> {
    return this.datesUntil(endDate).asSequence()
}

@JvmName("to")
infix fun LocalDate.to(that: LocalDate): LocalDateRange = LocalDateRange.of(this, that)

@JvmName("toNullable")
infix fun LocalDate?.to(that: LocalDate?): LocalDateRange? {
    return if (this == null || that == null) null
    else LocalDateRange.of(this, that)
}

operator fun LocalDateRange.component1(): LocalDate = this.start
operator fun LocalDateRange.component2(): LocalDate = this.end

fun ZoneId.toGmtString(atDate: Instant): String {
    val offsetSeconds = this.rules.getOffset(atDate).totalSeconds
    val plusSign = if (offsetSeconds > 0) "+" else ""
    val offsetString = when {
        offsetSeconds == 0 -> ""
        offsetSeconds.mod(3600) == 0 -> "${offsetSeconds / 3600}"
        else -> "${offsetSeconds.toFloat() / 3600}"
    }
    return "GMT$plusSign$offsetString"
}