package com.sama.api.config

import com.sama.common.NotFoundException
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Resolves [AuthUserId] annotated parameters to an Authenticated [UserEntity.id]
 */
@Component
class UserIdAttributeResolver(
    private val userRepository: UserRepository
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUserId::class.java) &&
                parameter.parameterType.equals(UserId::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory
    ): Long {
        val token = webRequest.userPrincipal as UsernamePasswordAuthenticationToken
        val email = token.principal as String
        return userRepository.findIdByEmail(email)
            ?: throw NotFoundException(UserEntity::class, "email", email)
    }
}
