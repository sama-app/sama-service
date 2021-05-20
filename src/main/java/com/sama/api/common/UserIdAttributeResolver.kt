package com.sama.api.common

import com.sama.common.NotFoundException
import com.sama.users.domain.User
import com.sama.users.domain.UserRepository
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Resolves [AuthUserId] annotated parameters to an Authenticated [User.id]
 */
@Component
class UserIdAttributeResolver(
    private val userRepository: UserRepository
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUserId::class.java) &&
                parameter.parameterType.equals(Long::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory
    ): Long {
        val token = webRequest.userPrincipal as UsernamePasswordAuthenticationToken
        val email = token.principal as String
        return userRepository.findIdByEmail(email = email)
            ?: throw NotFoundException(User::class, "email", email)
    }
}
