package com.sama.common

import com.sama.AppTestConfiguration
import com.sama.PersistenceConfiguration
import com.sama.PersistenceJdbcConfiguration
import java.time.Clock
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.Repository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        AppTestConfiguration::class,
        PersistenceConfiguration::class,
        PersistenceJdbcConfiguration::class
    ]
)
@Transactional
class BasePersistenceIT<T : Repository<out Any, out Any>> {

    companion object {
        @Container
        val container: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:13-alpine")
            .apply {
                withDatabaseName("sama-test")
                withUsername("test")
                withPassword("password")
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", container::getJdbcUrl)
            registry.add("spring.datasource.username", container::getUsername)
            registry.add("spring.datasource.password", container::getPassword)
        }
    }

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var underTest: T
}