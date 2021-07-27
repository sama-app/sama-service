package com.sama.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sama.api.common.ApiError
import com.sama.common.*
import com.sama.integration.google.GoogleInsufficientPermissionsException
import com.sama.integration.google.GoogleInvalidCredentialsException
import com.sama.users.domain.InactiveUserException
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.ZoneId
import java.time.zone.ZoneRulesException
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern


@Configuration
@Import(WebSecurityConfiguration::class, GlobalWebMvcExceptionHandler::class)
@EnableWebMvc
class WebMvcConfiguration(
    private val userIdAttributeResolver: UserIdAttributeResolver,
    @Value("\${sama.api.cors.allowed-origins}") private val corsAllowedOrigins: List<String>
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

    override fun addCorsMappings(registry: CorsRegistry) {
        if (corsAllowedOrigins.isEmpty()) {
            return
        }
        registry.addMapping("/**")
            .allowedOriginPatterns(*corsAllowedOrigins.toTypedArray())
            .allowedMethods("*")
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

    @ExceptionHandler(value = [ZoneRulesException::class, IllegalArgumentException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleBadRequest(ex: Exception, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), BAD_REQUEST, request)

    @ExceptionHandler(value = [DomainValidationException::class])
    @ResponseStatus(BAD_REQUEST)
    fun handleDomainValidation(ex: DomainValidationException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), BAD_REQUEST, request)

    @ExceptionHandler(value = [InactiveUserException::class, AuthenticationException::class])
    @ResponseStatus(UNAUTHORIZED)
    fun handleUnauthorized(ex: java.lang.Exception, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), UNAUTHORIZED, request)

    @ExceptionHandler(value = [GoogleInvalidCredentialsException::class, GoogleInsufficientPermissionsException::class])
    @ResponseStatus(FORBIDDEN)
    fun handleForbidden(ex: java.lang.Exception, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), FORBIDDEN, request)

    @ExceptionHandler(value = [NotFoundException::class])
    @ResponseStatus(NOT_FOUND)
    fun handleNotFound(ex: NotFoundException, request: WebRequest) =
        handleExceptionInternal(ex, null,HttpHeaders(), NOT_FOUND, request)

    @ExceptionHandler(value = [DomainIntegrityException::class])
    @ResponseStatus(CONFLICT)
    fun handleDomainIntegrity(ex: DomainIntegrityException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), CONFLICT, request)

    @ExceptionHandler(value = [DomainEntityStatusException::class])
    @ResponseStatus(GONE)
    fun handleDomainStatus(ex: DomainEntityStatusException, request: WebRequest) =
        handleExceptionInternal(ex, null, HttpHeaders(), GONE, request)

    @ExceptionHandler(value = [DomainInvalidActionException::class])
    @ResponseStatus(UNPROCESSABLE_ENTITY)
    fun handleDomainInvalidAction(ex: DomainInvalidActionException, request: WebRequest) =
        handleExceptionInternal(ex, null,HttpHeaders(), UNPROCESSABLE_ENTITY, request)

    override fun handleExceptionInternal(
        ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest
    ): ResponseEntity<Any> {
        val apiError = ApiError.create(status, ex, request)
        logger.info("Response [code=${apiError.status}, reason=${apiError.reason}, message=${apiError.message}]")

        return super.handleExceptionInternal(
            ex, body ?: apiError, headers, status, request
        )
    }
}