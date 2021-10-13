package com.sama.api.config

import com.sama.api.config.security.UserPrincipal
import java.util.UUID
import java.util.function.Predicate
import javax.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.AbstractRequestLoggingFilter

private const val REQUEST_ID_KEY = "request_id"
private const val USER_ID_KEY = "user_id"

class RequestLoggingFilter : AbstractRequestLoggingFilter() {
    var urlExclusionPredicate: Predicate<String> = Predicate { false }

    override fun shouldLog(request: HttpServletRequest): Boolean {
        return logger.isInfoEnabled && !urlExclusionPredicate.test(request.requestURI)
    }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().replace("-", ""))
        try {
            val (_, userId) = SecurityContextHolder.getContext().authentication?.principal as UserPrincipal
            userId?.let { MDC.put(USER_ID_KEY, "${it.id}") }
        } catch (ignored: Exception) {
        }
        logger.info(message)
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
        MDC.remove(REQUEST_ID_KEY)
        MDC.remove(USER_ID_KEY)
    }
}