package com.sama.common

import org.springframework.data.repository.CrudRepository
import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id)
    .orElseThrow { NotFoundException(T::class, id) }