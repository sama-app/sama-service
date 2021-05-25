package com.sama.common

import org.junit.jupiter.api.Assertions

fun <T, E : RuntimeException> Result<T>.assertThrows(expectedType: Class<E>) {
    Assertions.assertTrue(this.isFailure)
    Assertions.assertThrows(expectedType) { this.getOrThrow() }
}

fun <T> Result<T>.assertDoesNotThrowOrNull(): T {
    Assertions.assertTrue(this.isSuccess)
    Assertions.assertDoesNotThrow { this.getOrThrow() }
    val actual = this.getOrNull()
    Assertions.assertNotNull(actual)
    return actual!!
}
