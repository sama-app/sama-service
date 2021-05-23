package com.sama.api.common

import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import java.time.Instant
import java.util.*

data class ApiError(val status: Int, val error: String, val message: String, val path: String, val timestamp: Date) {
    companion object {
        fun create(httpStatus: HttpStatus, ex: Exception, request: WebRequest) = ApiError(
            httpStatus.value(),
            httpStatus.reasonPhrase,
            ex.message ?: "No message available",
            request.getDescription(false).substring(4),
            Date.from(Instant.now())
        )
    }
}