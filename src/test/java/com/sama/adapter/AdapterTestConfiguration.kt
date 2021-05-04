package com.sama.adapter

import com.sama.auth.domain.JwtConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val jwtSigningSecret = "secret"
const val jwtKeyId = "key-id"

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
}