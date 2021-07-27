package com.sama.common

import org.springframework.data.repository.CrudRepository
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.streams.asSequence

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id)
    .orElseThrow { NotFoundException(T::class, id) }

fun <T> MutableList<T>.mapMutating(transform: (T) -> T): MutableList<T> {
    Objects.requireNonNull(transform)
    val li: MutableListIterator<T> = this.listIterator()
    while (li.hasNext()) {
        li.set(transform.invoke(li.next()))
    }
    return this
}

fun <T> MutableList<T>.mapIndexed(transform: (Int, T) -> T): MutableList<T> {
    Objects.requireNonNull(transform)
    val li: MutableListIterator<T> = this.listIterator()
    while (li.hasNext()) {
        li.set(transform.invoke(li.nextIndex(), li.next()))
    }
    return this
}

fun DoubleArray.mapValues(transform: (Double) -> Double): DoubleArray {
    Objects.requireNonNull(transform)

    for ((index, item) in this.withIndex()) {
        this[index] = transform.invoke(item)
    }
    return this
}

fun DoubleArray.mapIndexed(transform: (Int, Double) -> Double): DoubleArray {
    Objects.requireNonNull(transform)

    var index = 0
    for (item in this) {
        this[index] = transform.invoke(index++, item)
    }
    return this
}


fun Long.toMinutes(): Duration = Duration.ofMinutes(this)

fun LocalDate.datesUtil(endDate: LocalDate): Sequence<LocalDate> {
    return this.datesUntil(endDate).asSequence()
}