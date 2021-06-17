package com.sama.api.config

import com.sama.api.config.security.JwtAuthorizationFilter
import com.sama.users.domain.JwtConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.Clock


@Configuration
@EnableWebSecurity
class WebSecurityConfiguration(
    @Qualifier("accessJwtConfiguration") private val accessJwtConfiguration: JwtConfiguration,
    private val clock: Clock
) : WebSecurityConfigurerAdapter() {

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
            .antMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/auth/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/meeting/by-code/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/meeting/by-code/**").permitAll()
            .antMatchers(HttpMethod.GET, "/__debug/auth/**").permitAll()
            // Actuator
            .antMatchers(HttpMethod.GET, "/__mon/**").permitAll()
            // All other requests require authentication
            .anyRequest().authenticated()
            .and()
            // Setup JWT authorization filter
            .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter::class.java)
    }
}