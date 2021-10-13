package com.sama.common

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.SECONDS
import liquibase.pro.packaged.T
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.Assertions

fun <T, E : RuntimeException> kotlin.Result<T>.assertThrows(expectedType: Class<E>) {
    Assertions.assertThrows(expectedType) { this.getOrThrow() }
    Assertions.assertTrue(this.isFailure)
}

fun <T> kotlin.Result<T>.assertDoesNotThrowOrNull(): T {
    Assertions.assertDoesNotThrow { this.getOrThrow() }
    Assertions.assertTrue(this.isSuccess)
    val actual = this.getOrNull()
    Assertions.assertNotNull(actual)
    return actual!!
}

/**
 * Apply [Comparator]s for [java.time] classes to be compared at [ChronoUnit.SECONDS] precision.
 */
fun <T> ObjectAssert<T>.usingLaxDateTimePrecision(): ObjectAssert<T> {
    return usingComparatorForType(
        Comparator.nullsLast { o1, o2 -> o1.truncatedTo(SECONDS).compareTo(o2.truncatedTo(SECONDS)) },
        ZonedDateTime::class.java
    ).usingComparatorForType(
        Comparator.nullsLast { o1, o2 -> o1.truncatedTo(SECONDS).compareTo(o2.truncatedTo(SECONDS)) },
        LocalDateTime::class.java
    ).usingComparatorForType(
        Comparator.nullsLast { o1, o2 -> o1.truncatedTo(SECONDS).compareTo(o2.truncatedTo(SECONDS)) },
        LocalTime::class.java
    )
}