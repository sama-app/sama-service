package com.sama.api.config

import org.springframework.web.filter.AbstractRequestLoggingFilter
import java.util.function.Predicate
import javax.servlet.http.HttpServletRequest

class RequestLoggingFilter : AbstractRequestLoggingFilter() {
    var urlExclusionPredicate: Predicate<String> = Predicate { false }

    override fun shouldLog(request: HttpServletRequest): Boolean {
        return logger.isInfoEnabled && !urlExclusionPredicate.test(request.requestURI)
    }

    override fun beforeRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
    }
}