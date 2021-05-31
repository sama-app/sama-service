package com.sama.common

import org.springframework.data.repository.CrudRepository
import java.time.Duration
import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id)
    .orElseThrow { NotFoundException(T::class, id) }

fun <T> MutableList<T>.mapMutating(transform: (T) -> T): MutableList<T>  {
    Objects.requireNonNull(transform)
    val li: MutableListIterator<T> = this.listIterator()
    while (li.hasNext()) {
        li.set(transform.invoke(li.next()))
    }
    return this
}

fun <T> MutableList<T>.mapIndexedMutating(transform: (Int, T) -> T): MutableList<T> {
    Objects.requireNonNull(transform)
    val li: MutableListIterator<T> = this.listIterator()
    while (li.hasNext()) {
        li.set(transform.invoke(li.nextIndex(), li.next()))
    }
    return this
}


fun Long.toMinutes(): Duration = Duration.ofMinutes(this)