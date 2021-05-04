package com.sama.configuration

import com.sama.adapter.auth.JwtAuthorizationFilter
import com.sama.auth.configuration.AccessJwtConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
class WebSecurityConfiguration(
    private val accessJwtConfiguration: AccessJwtConfiguration
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun jwtAuthorizationFilter(): JwtAuthorizationFilter {
        return JwtAuthorizationFilter(accessJwtConfiguration)
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
            .antMatchers(HttpMethod.GET, "/api/docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
            // Google OAuth2
            .antMatchers(HttpMethod.POST, "/api/auth/google-authorize").permitAll()
            .antMatchers(HttpMethod.GET, "/api/auth/google-oauth2").permitAll()
            // All other requests require authentication
            .anyRequest().authenticated().and()
            // Setup JWT authorization filter
            .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter::class.java)
    }
}