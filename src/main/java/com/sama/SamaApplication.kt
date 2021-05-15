package com.sama

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
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
class AppConfiguration {
    @Bean
    fun clock(): Clock {
        return Clock.systemUTC()
    }
}

@Configuration
@EnableJpaRepositories(basePackages = ["com.sama"])
@EntityScan("com.sama")
class PersistenceConfiguration