package com.sama.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@Configuration
class OpenApiConfiguration {

    @Bean
    fun openApi(): OpenAPI {
        SpringDocUtils.getConfig()
            .replaceWithClass(LocalTime::class.java, String::class.java)
            .replaceWithClass(ZoneId::class.java, String::class.java)
            .replaceWithClass(Locale::class.java, String::class.java)
        return OpenAPI().components(
            Components()
                .addSecuritySchemes(
                    "user-auth",
                    SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
        )
    }
}