package com.sama.api.config

import com.sama.api.config.security.UserPrincipal
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import com.sama.users.infrastructure.jpa.UserEntity
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
                // must be java.lang.Long, otherwise `UserId?` does not get matched
                (parameter.parameterType.equals(UserId::class.java))
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory
    ): UserId? {
        val token = webRequest.userPrincipal as UsernamePasswordAuthenticationToken?
        val userPrincipal = token?.principal as UserPrincipal?
            ?: return null

        return if (userPrincipal.userId != null) {
            userRepository.findIdByPublicIdOrThrow(userPrincipal.userId)
        } else {
            userRepository.findIdByEmailOrThrow(userPrincipal.email)
        }
    }
}
