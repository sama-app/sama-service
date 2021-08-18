package com.sama.common

import com.sama.AppTestConfiguration
import com.sama.IntegrationOverrides
import com.sama.api.config.security.UserPrincipal
import com.sama.users.domain.UserDetails
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
    private lateinit var initiatorUser: UserDetails
    private lateinit var recipientUser: UserDetails

    @BeforeEach
    fun setupUsers() {
        initiatorUser = userRepository.save(UserDetails(email = "initiator@meetsama.com", fullName = "Initiator User"))
        recipientUser = userRepository.save(UserDetails(email = "recipient@meetsama.com", fullName = "Recipient User"))
    }

    @AfterEach
    fun cleanupUsers() {
        userRepository.deleteAll()
    }

    fun initiator(): UserDetails {
        return initiatorUser
    }

    fun recipient(): UserDetails {
        return recipientUser
    }

    /**
     * Execute an action as a logged in "initiator" user
     */
    fun <T> asInitiator(executable: (user: UserDetails) -> T): T {
        return asUser(initiator(), executable)
    }

    /**
     * Execute an action as a logged in "recipient" user
     */
    fun <T> asRecipient(executable: (user: UserDetails) -> T): T {
        return asUser(recipient(), executable)
    }

    private fun <T> asUser(
        user: UserDetails,
        executable: (user: UserDetails) -> T,
    ): T {
        val userPrincipal = UserPrincipal(user.email, user.publicId)
        val auth = UsernamePasswordAuthenticationToken(userPrincipal, null, null)
        SecurityContextHolder.getContext().authentication = auth
        val result = executable.invoke(user)
        SecurityContextHolder.getContext().authentication = null
        return result
    }
}