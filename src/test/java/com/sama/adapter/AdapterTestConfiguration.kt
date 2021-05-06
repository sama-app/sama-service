package com.sama.adapter

import com.sama.adapter.auth.UserIdAttributeResolver
import com.sama.auth.domain.AuthUser
import com.sama.auth.domain.AuthUserRepository
import com.sama.auth.domain.JwtConfiguration
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val jwtSigningSecret = "dummy-access-secret-for-development"
const val jwtKeyId = "dummy-access-key-id-for-development"

@Configuration
class AdapterTestConfiguration {

    @Bean
    fun accessJwtConfiguration(): JwtConfiguration {
        return object : JwtConfiguration {
            override val signingSecret: String
                get() = jwtSigningSecret
            override val expirationSec: Long
                get() = 3600
            override val keyId: String
                get() = jwtKeyId
        }
    }

    @Bean
    fun fixedUserIdAttributeResolver(): UserIdAttributeResolver {
        var authUserRepository = mock(AuthUserRepository::class.java)
        whenever(authUserRepository.findIdByEmail(any())).thenReturn(1)
        return UserIdAttributeResolver(authUserRepository)
    }
}