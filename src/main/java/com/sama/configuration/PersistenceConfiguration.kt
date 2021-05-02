package com.sama.configuration

import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJpaRepositories(basePackages = ["com.sama"])
@EntityScan("com.sama")
class PersistenceConfiguration