package com.sama

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication(
    scanBasePackages = ["com.sama"]
)
@ConfigurationPropertiesScan
class SamaApplication

fun main(args: Array<String>) {
    SpringApplication.run(SamaApplication::class.java, *args)
}