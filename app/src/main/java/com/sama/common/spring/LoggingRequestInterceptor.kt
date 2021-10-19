package com.sama.common.spring

import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class LoggingRequestInterceptor(private val headerBlacklist: List<String> = emptyList()) : ClientHttpRequestInterceptor {
    private val logger = LogFactory.getLog(javaClass)

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        logger.info(
            """
            -------------- REQUEST  --------------
            ${request.method} ${request.uri}
            ${request.headers.mapValues { (k, v) -> if (k in headerBlacklist) "<Not Logged>" else v }}
            
            """.trimIndent()
        )

        val response = execution.execute(request, body)

        logger.info(
            """
            -------------- RESPONSE --------------
            ${response.statusCode}
            ${response.headers.mapValues { (k, v) -> if (k in headerBlacklist) "<Not Logged>" else v }}
            
            """.trimIndent()
        )

        return response
    }
}