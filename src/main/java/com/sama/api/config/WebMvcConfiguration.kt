package com.sama.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sama.api.common.UserIdAttributeResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
@Import(WebSecurityConfiguration::class)
@EnableWebMvc
class WebMvcConfiguration(
    private val userIdAttributeResolver: UserIdAttributeResolver
) : WebMvcConfigurer {
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

    fun jsonConverter(): MappingJackson2HttpMessageConverter {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // enable date time serialization to string
        return MappingJackson2HttpMessageConverter(objectMapper)
    }

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(StringHttpMessageConverter())
        converters.add(jsonConverter())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(DeviceResolverHandlerInterceptor())
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(userIdAttributeResolver)
    }
}