package com.sama.integration.sentry

import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> CoroutineScope.asyncTraced(block: CoroutineScope.() -> T): Deferred<T> {
    val oldState = Sentry.getCurrentHub()
    val newHub = oldState.clone()
    return async {
        Sentry.setCurrentHub(newHub)
        try {
            block.invoke(this)
        } finally {
            // there might be race conditions here leading to trace mappings that are not accurate
            Sentry.setCurrentHub(oldState)
        }
    }
}

@Throws(InterruptedException::class)
fun <T> runBlockingTraced(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
    val oldState = Sentry.getCurrentHub()
    val newHub = oldState.clone()
    return runBlocking(context) {
        Sentry.setCurrentHub(newHub)
        try {
            block.invoke(this)
        } finally {
            // there might be race conditions here leading to trace mappings that are not accurate
            Sentry.setCurrentHub(oldState)
        }
    }
}

inline fun <T> sentrySpan(transaction: String? = null, method: String, function: Function0<T>): T {
    var span = Sentry.getSpan()
    if (span == null) {
        span = Sentry.startTransaction(transaction ?: "unmapped", method)
    }
    val innerSpan = span.startChild(method)
    try {
        return function.invoke()
    } catch (e: Exception) {
        innerSpan.throwable = e;
        throw e
    } finally {
        innerSpan.finish()
    }
}