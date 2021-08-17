package com.sama.integration.sentry

import io.sentry.Sentry

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