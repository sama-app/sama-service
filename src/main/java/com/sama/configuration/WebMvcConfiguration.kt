package com.sama.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
@Import(WebSecurityConfiguration::class)
@EnableWebMvc
class WebMvcConfiguration : WebMvcConfigurer {
    private val headerBlacklist = listOf("authorization", "cookie")

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludeClientInfo(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(true)
        filter.setHeaderPredicate { s: String -> !headerBlacklist.contains(s.toLowerCase()) }
        return filter
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(DeviceResolverHandlerInterceptor())
    }
}