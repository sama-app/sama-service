package com.sama

import com.sama.integration.google.calendar.infrastructure.GoogleAccountIdReader
import com.sama.integration.google.calendar.infrastructure.GoogleAccountIdWriter
import io.undertow.UndertowOptions.ENABLE_HTTP2
import java.time.Clock
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.relational.core.mapping.NamingStrategy
import org.springframework.scheduling.annotation.EnableScheduling


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
class PersistenceJdbcConfiguration : AbstractJdbcConfiguration() {

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(GoogleAccountIdWriter(), GoogleAccountIdReader()))
    }

    @Bean
    fun namingStrategy(): NamingStrategy = object : NamingStrategy {
        override fun getSchema(): String {
            // The default schema provided by the JDBC connection is 'sama'. For tables in that schema
            // use @Table(TABLE_NAME) on the entity.
            // Of course, this won't work if we have three or more schemas.
            return "gcal"
        }
    }
}

@Configuration
@EnableCaching
class CacheConfiguration