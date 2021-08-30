package com.sama.common

import java.time.Duration
import java.time.LocalDate
import java.util.Objects
import java.util.Optional
import kotlin.streams.asSequence
import org.springframework.data.repository.CrudRepository
import org.threeten.extra.LocalDateRange

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


/**
 * Returns a sequence of snapshots given a [chunkCutOffPredicate] that determines where each
 * chunk ends.
 */
fun <T> Sequence<T>.chunkedBy(chunkCutOffPredicate: (T, T) -> Boolean): Sequence<List<T>> {
    return Sequence { chunkedByIterator(iterator(), chunkCutOffPredicate) }
}

/**
 * Returns a sequence of snapshots given a [chunkCutOffPredicate] that determines where each
 * chunk ends and perform a transformation on the result chunks.
 */
fun <T, R> Sequence<T>.chunkedBy(chunkCutOffPredicate: (T, T) -> Boolean, transform: (Int, List<T>) -> R): Sequence<R> {
    return Sequence { chunkedByIterator(iterator(), chunkCutOffPredicate) }.mapIndexed(transform)
}

fun <T> chunkedByIterator(
    iterator: Iterator<T>,
    chunkCutOffPredicate: (T, T) -> Boolean,
): Iterator<List<T>> {
    return iterator<List<T>> {
        val bufferInitialCapacity = 4
        var buffer = ArrayList<T>(bufferInitialCapacity)
        for (e in iterator) {
            if (buffer.isNotEmpty()) {
                val cutOff = chunkCutOffPredicate.invoke(buffer.last(), e)
                if (cutOff) {
                    yield(buffer)
                    buffer = ArrayList(bufferInitialCapacity)
                }
            }
            buffer.add(e)
        }
        if (buffer.isNotEmpty()) {
            yield(buffer)
        }
    }
}


fun Long.toMinutes(): Duration = Duration.ofMinutes(this)

fun LocalDate.datesUtil(endDate: LocalDate): Sequence<LocalDate> {
    return this.datesUntil(endDate).asSequence()
}

operator fun LocalDateRange.component1(): LocalDate = this.start
operator fun LocalDateRange.component2(): LocalDate = this.end

@JvmName("to")
infix fun LocalDate.to(that: LocalDate): LocalDateRange = LocalDateRange.of(this, that)

@JvmName("toNullable")
infix fun LocalDate?.to(that: LocalDate?): LocalDateRange? {
    return if (this == null || that == null) null
    else LocalDateRange.of(this, that)
}