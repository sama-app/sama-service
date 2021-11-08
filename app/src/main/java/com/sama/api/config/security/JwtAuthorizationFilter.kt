package com.sama.api.config.security

import com.auth0.jwt.exceptions.TokenExpiredException
import com.sama.users.domain.Jwt
import com.sama.users.domain.JwtConfiguration
import com.sama.users.domain.UserPublicId
import java.io.IOException
import java.time.Clock
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter


/**
 * Authorizes API requests by extracting a JWT token and Authorization header in format "Bearer $token"
 */
class JwtAuthorizationFilter(
    private val accessJwtConfiguration: JwtConfiguration,
    private val clock: Clock
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val jwt = getJwtFromHeader(request) ?: getJwtFromCookie(request)
        jwt?.let { token ->
            Jwt.verified(token, accessJwtConfiguration, clock)
                .onSuccess {
                    val userPrincipal = UserPrincipal(it.userEmail(), it.userId())
                    val auth = UsernamePasswordAuthenticationToken(userPrincipal, null, null)
                    SecurityContextHolder.getContext().authentication = auth
                }
                .onFailure {
                    if (it is TokenExpiredException) {
                        // Set Principal if token is valid but expired so we can act on it
                        Jwt.raw(token).getOrNull()
                            ?.let { jwt ->
                                val userPrincipal = UserPrincipal(jwt.userEmail(), jwt.userId())
                                // this means NOT authenticated
                                val auth = UsernamePasswordAuthenticationToken(userPrincipal, null)
                                SecurityContextHolder.getContext().authentication = auth
                            }
                    }
                    logger.debug("jwt authentication error:", it)
                }
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

    private fun getJwtFromCookie(request: HttpServletRequest): String? {
        return request.cookies?.firstOrNull { it.name == "sama.access" }?.value
    }
}

data class UserPrincipal(val email: String, val userId: UserPublicId?)

