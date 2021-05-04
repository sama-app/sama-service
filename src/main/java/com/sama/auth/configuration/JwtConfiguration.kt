package com.sama.auth.configuration

import com.sama.auth.domain.JwtConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.auth.jwt.access")
@Qualifier("accessJwtConfiguration")
class AccessJwtConfiguration(
    override val signingSecret: String,
    override val expirationSec: Long,
    override val keyId: String,
) : JwtConfiguration

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.auth.jwt.refresh")
@Qualifier("refreshJwtConfiguration")
data class RefreshJwtConfiguration(
    override val signingSecret: String,
    override val expirationSec: Long,
    override val keyId: String,
) : JwtConfiguration