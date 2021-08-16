package com.sama

import io.undertow.UndertowOptions.ENABLE_HTTP2
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock


@SpringBootApplication(
    scanBasePackages = ["com.sama"]
)
@ConfigurationPropertiesScan
class SamaApplication

fun main(args: Array<String>) {
    SpringApplication.run(SamaApplication::class.java, *args)
}

@Configuration
@EnableScheduling
class AppConfiguration {
    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }

    @Bean
    fun undertowServletWebServerFactory(): UndertowServletWebServerFactory {
        val factory = UndertowServletWebServerFactory()
        factory.addBuilderCustomizers(
            UndertowBuilderCustomizer {
                it.setServerOption(ENABLE_HTTP2, true)
            })
        return factory
    }
}

@Configuration
@EnableJpaRepositories(basePackages = ["com.sama"])
@EnableJdbcRepositories(basePackages = ["com.sama"])
@EntityScan("com.sama")
class PersistenceConfiguration

@Configuration
@EnableCaching
class CacheConfiguration