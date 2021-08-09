package com.sama.common

import com.sama.AppTestConfiguration
import com.sama.IntegrationOverrides
import com.sama.api.config.security.UserPrincipal
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [AppTestConfiguration::class, IntegrationOverrides::class])
class BaseApplicationTest {

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
    lateinit var userRepository: UserRepository


    // test data
    private lateinit var initiatorUser: UserEntity
    private lateinit var recipientUser: UserEntity

    @BeforeEach
    fun setupUsers() {
        initiatorUser = userRepository.save(
            UserEntity("initiator@meetsama.com").apply {
                fullName = "Initiator User"
            })
        recipientUser = userRepository.save(
            UserEntity("recipient@meetsama.com").apply {
                fullName = "Recipient User"
            })
        userRepository.flush()
    }

    @AfterEach
    fun cleanupUsers() {
        userRepository.deleteAll()
        userRepository.flush()
    }


    fun initiator(): UserEntity {
        return initiatorUser
    }

    fun recipient(): UserEntity {
        return recipientUser
    }

    /**
     * Execute an action as a logged in "initiator" user
     */
    fun <T> asInitiator(executable: (user: UserEntity) -> T): T {
        return asUser(initiator(), executable)
    }

    /**
     * Execute an action as a logged in "recipient" user
     */
    fun <T> asRecipient(executable: (user: UserEntity) -> T): T {
        return asUser(recipient(), executable)
    }

    private fun <T> asUser(
        user: UserEntity,
        executable: (user: UserEntity) -> T,
    ): T {
        val userPrincipal = UserPrincipal(user.email, user.publicId)
        val auth = UsernamePasswordAuthenticationToken(userPrincipal, null, null)
        SecurityContextHolder.getContext().authentication = auth
        val result = executable.invoke(user)
        SecurityContextHolder.getContext().authentication = null
        return result
    }
}