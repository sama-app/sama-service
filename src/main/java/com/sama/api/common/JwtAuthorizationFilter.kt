package com.sama.api.common

import com.sama.users.domain.Jwt
import com.sama.users.domain.JwtConfiguration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Authorizes API requests by extracting a JWT token and Authorization header in format "Bearer $token"
 */
class JwtAuthorizationFilter(private val accessJwtConfiguration: JwtConfiguration) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        getJwtFromHeader(request)
            ?.let {
                runCatching { Jwt(it, accessJwtConfiguration).userEmail() }
                    .onSuccess {
                        val token = UsernamePasswordAuthenticationToken(it, null, null)
                        SecurityContextHolder.getContext().authentication = token
                    }
                    .onFailure { logger.info("jwt authentication error:", it) }
            }

        chain.doFilter(request, response)
    }

    private fun getJwtFromHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }
}

