package com.sama.api.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.sama.api.common.ApiError
import com.sama.common.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.zone.ZoneRulesException
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern


@Configuration
@Import(WebSecurityConfiguration::class, GlobalWebMvcExceptionHandler::class)
@EnableWebMvc
class WebMvcConfiguration(
    private val userIdAttributeResolver: UserIdAttributeResolver
) : WebMvcConfigurer {
    private val headerBlacklist = listOf("authorization", "cookie")
    private val urlBlacklist = Pattern.compile("/__mon/*")

    @Bean
    fun logFilter(): RequestLoggingFilter {
        val filter = RequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludeClientInfo(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(true)
        filter.urlExclusionPredicate = Predicate { url: String -> urlBlacklist.matcher(url).find() }
        filter.setHeaderPredicate { header: String -> !headerBlacklist.contains(header.lowercase()) }
        return filter
    }

    fun jsonConverter(): MappingJackson2HttpMessageConverter {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")))
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
        ResponseEntity(ApiError.create(NOT_FOUND, ex, request), HttpHeaders(), NOT_FOUND)

    @ExceptionHandler(value = [ZoneRulesException::class, IllegalArgumentException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleBadRequest(ex: Exception, request: WebRequest) =
        ResponseEntity(ApiError.create(BAD_REQUEST, ex, request), HttpHeaders(), BAD_REQUEST)

    @ExceptionHandler(value = [DomainValidationException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleDomainValidation(ex: DomainValidationException, request: WebRequest) =
        ResponseEntity(ApiError.create(BAD_REQUEST, ex, request), HttpHeaders(), BAD_REQUEST)

    @ExceptionHandler(value = [DomainIntegrityException::class])
    @ResponseStatus(CONFLICT)
    fun handleDomainIntegrity(ex: DomainIntegrityException, request: WebRequest) =
        ResponseEntity(ApiError.create(CONFLICT, ex, request), HttpHeaders(), CONFLICT)

    @ExceptionHandler(value = [DomainEntityStatusException::class])
    @ResponseStatus(GONE)
    fun handleDomainStatus(ex: DomainEntityStatusException, request: WebRequest) =
        ResponseEntity(ApiError.create(GONE, ex, request), HttpHeaders(), GONE)

    @ExceptionHandler(value = [DomainInvalidActionException::class])
    @ResponseStatus(UNPROCESSABLE_ENTITY)
    fun handleDomainInvalidAction(ex: DomainInvalidActionException, request: WebRequest) =
        ResponseEntity(ApiError.create(UNPROCESSABLE_ENTITY, ex, request), HttpHeaders(), UNPROCESSABLE_ENTITY)

    override fun handleExceptionInternal(
        ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest
    ): ResponseEntity<Any> = super.handleExceptionInternal(
        ex, body ?: ApiError.create(status, ex, request), headers, status, request
    )
}