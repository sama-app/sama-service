package com.sama.common

enum class Kind {
    Success,
    Failure
}

class ResultTypeException(error: Any?) : RuntimeException("Result error does not extend Throwable: $error")

inline fun <V, U, reified E> Result<V, E>.map(transform: (V) -> U): Result<U, E> = try {
    when (this) {
        is Result.Success -> Result.success(transform(value))
        is Result.Failure -> this
    }
} catch (ex: Exception) {
    when (ex) {
        is E -> Result.failure(ex)
        else -> throw ex
    }
}

inline fun <V, reified E, reified EE : Throwable> Result<V, E>.mapError(transform: (E) -> EE): Result<V, EE> = try {
    when (this) {
        is Result.Success -> Result.success(value)
        is Result.Failure -> Result.failure(transform(error))
    }
} catch (ex: Exception) {
    when (ex) {
        is EE -> Result.failure(ex)
        else -> throw ex
    }
}

inline fun <V, U, reified E, reified EE : Throwable> Result<V, E>.mapBoth(
    transformSuccess: (V) -> U,
    transformFailure: (E) -> EE
): Result<U, EE> = try {
    when (this) {
        is Result.Success -> Result.success(transformSuccess(value))
        is Result.Failure -> Result.failure(transformFailure(error))
    }
} catch (ex: Exception) {
    when (ex) {
        is EE -> Result.failure(ex)
        else -> throw ex
    }
}


sealed class Result<out V, out E> {

    open operator fun component1(): V? = null
    open operator fun component2(): E? = null

    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X = when (this) {
        is Success -> success(value)
        is Failure -> failure(error)
    }

    abstract fun getOrThrow(): V

    abstract val kind: Kind

    class Success<out V : Any?> internal constructor(val value: V) : Result<V, Nothing>() {

        override val kind: Kind = Kind.Success

        override fun component1(): V = value

        override fun getOrThrow(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*> && value == other.value
        }
    }

    class Failure<out E> internal constructor(val error: E) : Result<Nothing, E>() {

        override val kind: Kind = Kind.Failure

        override fun component2(): E = error

        override fun getOrThrow() = if (error is Throwable)
            throw error else
            throw ResultTypeException(error)

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*> && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun <E : Throwable> failure(throwable: E) = Failure(throwable)
        fun <V> success(value: V) = Success(value)
    }
}