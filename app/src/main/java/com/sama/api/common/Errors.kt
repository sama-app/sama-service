package com.sama.api.common

import com.sama.common.HasReason
import java.time.Instant
import java.util.Date
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest

data class ApiError(val status: Int, val reason: String, val message: String, val path: String, val timestamp: Date) {
    companion object {
        fun create(
            httpStatus: HttpStatus,
            ex: Exception,
            request: WebRequest,
            includeErrorMessage: Boolean = false,
        ): ApiError {
            val reason = if (ex is HasReason) {
                ex.reason
            } else {
                httpStatus.reasonPhrase.lowercase().replace(' ', '_')
            }

            return ApiError(
                httpStatus.value(),
                reason,
                if (includeErrorMessage) ex.message ?: "No message available" else "",
                request.getDescription(false).substring(4),
                Date.from(Instant.now())
            )
        }
    }
}