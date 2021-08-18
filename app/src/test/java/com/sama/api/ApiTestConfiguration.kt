package com.sama.api

import com.sama.api.config.UserIdAttributeResolver
import com.sama.users.domain.JwtConfiguration
import com.sama.users.domain.UserRepository
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

const val jwtSigningSecret = "secret"
const val jwtKeyId = "key-id"

@Configuration
class ApiTestConfiguration {

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
        var authUserRepository = mock(UserRepository::class.java)
        whenever(authUserRepository.findIdByPublicIdOrThrow(any())).thenReturn(1)
        whenever(authUserRepository.findIdByEmailOrThrow(any())).thenReturn(1)
        return UserIdAttributeResolver(authUserRepository)
    }

    @Bean
    fun fixedClock(): Clock {
        val fixedDate = LocalDate.of(2021, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return Clock.fixed(fixedDate, ZoneId.systemDefault());
    }
}