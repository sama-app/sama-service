package com.sama.integration.mailerlite

import com.sama.common.spring.LoggingRequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

private const val API_KEY_HEADER = "X-MailerLite-ApiKey"

@Configuration
class MailerLiteConfiguration {

    @Bean
    fun mailerLiteRestTemplate(
        @Value("\${integration.mailerlite.api-url}") apiUrl: String,
        @Value("\${integration.mailerlite.api-key}") apiKey: String
    ): RestTemplate {
        val apiKeyInterceptor = ClientHttpRequestInterceptor { request, body, ex ->
            request.headers.set(API_KEY_HEADER, apiKey)
            request.headers.set("Accept", "application/json")
            ex.execute(request, body);
        }

        return RestTemplate().apply {
            requestFactory = HttpComponentsClientHttpRequestFactory()
            uriTemplateHandler = DefaultUriBuilderFactory(apiUrl)
            interceptors = listOf(apiKeyInterceptor, LoggingRequestInterceptor(headerBlacklist = listOf(API_KEY_HEADER)))
        }
    }
}