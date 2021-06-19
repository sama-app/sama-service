package com.sama.common

import liquibase.pro.packaged.E
import org.junit.jupiter.api.Assertions
import java.lang.Exception

fun <T, E : RuntimeException> Result<T>.assertThrows(expectedType: Class<E>) {
    Assertions.assertThrows(expectedType) { this.getOrThrow() }
    Assertions.assertTrue(this.isFailure)
}

fun <T> Result<T>.assertDoesNotThrowOrNull(): T {
    Assertions.assertDoesNotThrow { this.getOrThrow() }
    Assertions.assertTrue(this.isSuccess)
    val actual = this.getOrNull()
    Assertions.assertNotNull(actual)
    return actual!!
}
