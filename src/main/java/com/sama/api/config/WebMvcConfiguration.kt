package com.sama.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sama.api.common.ApiError
import com.sama.common.DomainValidationException
import com.sama.common.NotFoundException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.zone.ZoneRulesException


@Configuration
@Import(WebSecurityConfiguration::class, GlobalWebMvcExceptionHandler::class)
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

@ControllerAdvice
class GlobalWebMvcExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [NotFoundException::class])
    @ResponseStatus(NOT_FOUND)
    fun handleNotFound(ex: NotFoundException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), NOT_FOUND, request)

    @ExceptionHandler(value = [ZoneRulesException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleBadRequest(ex: ZoneRulesException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), BAD_REQUEST, request)

    @ExceptionHandler(value = [DomainValidationException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleDomainValidation(ex: DomainValidationException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), BAD_REQUEST, request)

    override fun handleExceptionInternal(
        ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest
    ): ResponseEntity<Any> = super.handleExceptionInternal(
        ex, body ?: ApiError.create(status, ex, request), headers, status, request
    )
}