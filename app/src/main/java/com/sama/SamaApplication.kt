package com.sama

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.TransactionManager
import java.time.Clock
import javax.sql.DataSource


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
}

@Configuration
@EnableJpaRepositories(basePackages = ["com.sama"])
@EnableJdbcRepositories(basePackages = ["com.sama"])
@EntityScan("com.sama")
class PersistenceConfiguration

@Configuration
@EnableCaching
class CacheConfiguration