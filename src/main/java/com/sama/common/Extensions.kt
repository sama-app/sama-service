package com.sama.common

import org.springframework.data.repository.CrudRepository
import java.time.Duration
import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id)
    .orElseThrow { NotFoundException(T::class, id) }

fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
    return map { if (block(it)) newValue else it }
}

fun Long.toMinutes(): Duration = Duration.ofMinutes(this)