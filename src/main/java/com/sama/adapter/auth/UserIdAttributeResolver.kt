package com.sama.adapter.auth

import com.sama.auth.domain.AuthUserRepository
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class UserIdAttributeResolver(
    private val authUserRepository: AuthUserRepository
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(UserId::class.java) &&
                parameter.parameterType.equals(Long::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory
    ): Long {
        val token = webRequest.userPrincipal as UsernamePasswordAuthenticationToken
        return authUserRepository.findIdByEmail(email = token.principal as String)!!
    }
}

annotation class UserId