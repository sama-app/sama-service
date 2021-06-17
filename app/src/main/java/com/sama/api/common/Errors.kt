package com.sama.api.common

import com.sama.common.HasReason
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import java.time.Instant
import java.util.*

data class ApiError(val status: Int, val reason: String, val message: String, val path: String, val timestamp: Date) {
    companion object {
        fun create(httpStatus: HttpStatus, ex: Exception, request: WebRequest): ApiError {
            val reason = if (ex is HasReason) {
                ex.reason
            } else {
                httpStatus.reasonPhrase.lowercase().replace(' ', '_')
            }

            return ApiError(
                httpStatus.value(),
                reason,
                ex.message ?: "No message available",
                request.getDescription(false).substring(4),
                Date.from(Instant.now())
            )
        }
    }
}