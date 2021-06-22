package com.sama.integration.sentry

import javax.servlet.http.HttpServletRequest

import io.sentry.SentryOptions.TracesSamplerCallback
import io.sentry.SamplingContext
import org.springframework.stereotype.Component

@Component
class CustomTracesSamplerCallback : TracesSamplerCallback {

    override fun sample(context: SamplingContext): Double? {
        val customSamplingContext = context.customSamplingContext
        if (customSamplingContext != null) {
            // exclude monitoring URL
            val request = customSamplingContext["request"] as HttpServletRequest
            val exclude = request.requestURI.contains("/__mon", true)
            if (exclude) {
                return 0.0
            }
        }
        return null
    }
}