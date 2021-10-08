package com.sama.api.config

import com.sama.api.config.security.JwtAuthorizationFilter
import com.sama.users.domain.JwtConfiguration
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.time.Clock
import javax.servlet.http.HttpServletResponse
import liquibase.pro.packaged.it


@Configuration
@EnableWebSecurity
class WebSecurityConfiguration(
    @Qualifier("accessJwtConfiguration") private val accessJwtConfiguration: JwtConfiguration,
    private val handlerExceptionResolver: HandlerExceptionResolver,
    private val clock: Clock
) : WebSecurityConfigurerAdapter() {
    private val logger = LogFactory.getLog(WebSecurityConfigurerAdapter::class.java)

    @Bean
    fun jwtAuthorizationFilter(): JwtAuthorizationFilter {
        return JwtAuthorizationFilter(accessJwtConfiguration, clock)
    }

    override fun configure(httpSecurity: HttpSecurity) {
        httpSecurity
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(STATELESS).and()
            .authorizeRequests()
            // OpenApi Spec
            .antMatchers(HttpMethod.GET, "/api/docs/**", "/api/swagger-ui.html", "/api/swagger-ui/**").permitAll()
            // Google OAuth2
            .antMatchers(HttpMethod.POST, "/api/auth/link-google-account").authenticated()
            .antMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/meeting/by-code/*").permitAll()
            .antMatchers(HttpMethod.POST, "/api/meeting/by-code/*/confirm").permitAll()
            .antMatchers(HttpMethod.POST, "/api/integration/google/channel-notification").permitAll()
            .antMatchers(HttpMethod.GET, "/api/__debug/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/__debug/auth/link-google-account").authenticated()
            // Actuator
            .antMatchers(HttpMethod.GET, "/__mon/**").permitAll()
            // All other requests require authentication
            .anyRequest().authenticated()
            // Catch Spring Security exceptions and convert them using GlobalWebMvcExceptionHandler
            .and().exceptionHandling {
                it.authenticationEntryPoint { request, response, authException ->
                    val result = handlerExceptionResolver.resolveException(request, response, null, authException)
                    if (result == null) {
                        logger.warn("Unhandled auth exception", authException)
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unhandled_message")
                    }
                }
            }
            // Setup JWT authorization filter
            .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter::class.java)
    }
}