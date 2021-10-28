package com.sama.common

import com.sama.AppTestConfiguration
import com.sama.IntegrationOverrides
import com.sama.api.config.security.UserPrincipal
import com.sama.users.application.UserInternalDTO
import com.sama.users.application.toDTO
import com.sama.users.application.toInternalDTO
import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsRepository
import java.time.ZoneOffset.UTC
import java.util.Locale.ENGLISH
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
class BaseApplicationIntegrationTest {

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

    @Autowired
    lateinit var userSettingsRepository: UserSettingsRepository

    // test data
    private lateinit var initiatorUser: UserDetails
    private lateinit var initiatorSettings: UserSettings
    private lateinit var recipientUser: UserDetails
    private lateinit var recipientSettings: UserSettings

    @BeforeEach
    fun setupUsers() {
        initiatorUser = userRepository.save(
            UserDetails(
                email = "initiator@meetsama.com",
                fullName = "Initiator User",
                active = true
            )
        )
        initiatorSettings = userSettingsRepository.save(
            UserSettings(
                initiatorUser.id!!,
                ENGLISH, UTC, true, emptyMap(), false, emptySet()
            )
        )

        recipientUser = userRepository.save(
            UserDetails(
                email = "recipient@meetsama.com",
                fullName = "Recipient User",
                active = true
            )
        )
        recipientSettings = userSettingsRepository.save(
            UserSettings(
                recipientUser.id!!,
                ENGLISH, UTC, true, emptyMap(), false, emptySet()
            )
        )
    }

    @AfterEach
    fun cleanupUsers() {
        userRepository.deleteAll()
    }

    fun initiator() = initiatorUser.toInternalDTO(initiatorSettings.toDTO())

    fun recipient() = recipientUser.toInternalDTO(recipientSettings.toDTO())

    /**
     * Execute an action as a logged in "initiator" user
     */
    fun <T> asInitiator(executable: (user: UserInternalDTO) -> T): T {
        return asUser(initiator(), executable)
    }

    /**
     * Execute an action as a logged in "recipient" user
     */
    fun <T> asRecipient(executable: (user: UserInternalDTO) -> T): T {
        return asUser(recipient(), executable)
    }

    private fun <T> asUser(
        user: UserInternalDTO,
        executable: (user: UserInternalDTO) -> T,
    ): T {
        val userPrincipal = UserPrincipal(user.email, user.publicId)
        val auth = UsernamePasswordAuthenticationToken(userPrincipal, null, null)
        SecurityContextHolder.getContext().authentication = auth
        val result = executable.invoke(user)
        SecurityContextHolder.getContext().authentication = null
        return result
    }
}