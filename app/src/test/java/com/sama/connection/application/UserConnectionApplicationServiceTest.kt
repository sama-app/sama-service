package com.sama.connection.application

import com.sama.AppTestConfiguration
import com.sama.IntegrationOverrides
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import java.util.UUID.fromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
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
class UserConnectionApplicationServiceTest {

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
    lateinit var underTest: UserConnectionApplicationService

    // test data
    private val userOne =
        UserEntity(1L, fromString("37f88284-f195-47e6-8f69-451395aa9db1"), "one@meetsama.com").apply {
            fullName = "One"
        }
    private val userTwo =
        UserEntity(2L, fromString("c718c4d6-2b52-4baf-9e4b-836bc450f47d"), "two@meetsama.com").apply {
            fullName = "Two"
        }

    @BeforeEach
    fun setupUsers() {
        userRepository.save(userOne)
        userRepository.save(userTwo)
        userRepository.flush()
    }

    @AfterEach
    fun cleanupUsers() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `connect two users via request`() {
        val connectionRequest =
            underTest.createConnectionRequest(userOne.id, CreateConnectionRequestCommand(userTwo.publicId))

        asUser(userTwo) {
            underTest.approveConnectionRequest(it.id, connectionRequest.connectionRequestId)
        }

        asUser(userOne) {
            val connections = underTest.findUserConnections(it.id)
            val connectionRequests = underTest.findConnectionRequests(it.id)
            assertThat(connections.connectedUsers)
                .containsExactly(UserDTO(userTwo.publicId, "two@meetsama.com", "Two"))
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }

        asUser(userTwo) {
            val connections = underTest.findUserConnections(it.id)
            val connectionRequests = underTest.findConnectionRequests(it.id)
            assertThat(connections.connectedUsers)
                .containsExactly(UserDTO(userOne.publicId, "one@meetsama.com", "One"))
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }
    }

    @Test
    fun `cannot manipulate own connection request`() {
        asUser(userOne) {
            val connectionRequest =
                underTest.createConnectionRequest(it.id, CreateConnectionRequestCommand(userTwo.publicId))

            assertThrows<AccessDeniedException> {
                underTest.approveConnectionRequest(it.id, connectionRequest.connectionRequestId)
            }

            assertThrows<AccessDeniedException> {
                underTest.rejectConnectionRequest(it.id, connectionRequest.connectionRequestId)
            }
        }
    }

    @Test
    fun `reject user connection request`() {
        val connectionRequest =
            underTest.createConnectionRequest(userOne.id, CreateConnectionRequestCommand(userTwo.publicId))

        asUser(userTwo) {
            underTest.rejectConnectionRequest(userTwo.id, connectionRequest.connectionRequestId)
        }

        asUser(userOne) {
            val connections = underTest.findUserConnections(userOne.id)
            assertThat(connections.connectedUsers).isEmpty()
        }

        asUser(userTwo) {
            val connections = underTest.findUserConnections(userTwo.id)
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `disconnect users after connection`() {
        val connectionRequest =
            underTest.createConnectionRequest(userOne.id, CreateConnectionRequestCommand(userTwo.publicId))

        asUser(userTwo) {
            underTest.approveConnectionRequest(userTwo.id, connectionRequest.connectionRequestId)
        }

        asUser(userOne) {
            underTest.removeUserConnection(userOne.id, RemoveUserConnectionCommand(userTwo.publicId))
        }

        asUser(userOne) {
            val connections = underTest.findUserConnections(userOne.id)
            assertThat(connections.connectedUsers).isEmpty()
        }

        asUser(userTwo) {
            val connections = underTest.findUserConnections(userTwo.id)
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `list connection requests for two users`() {
        val connectionRequest =
            underTest.createConnectionRequest(userOne.id, CreateConnectionRequestCommand(userTwo.publicId))

        val connectionRequestDTO = ConnectionRequestDTO(
            connectionRequest.connectionRequestId,
            initiator = UserDTO(userOne.publicId, userOne.email, userOne.fullName),
            recipient = UserDTO(userTwo.publicId, userTwo.email, userTwo.fullName)
        )

        asUser(userOne) {
            val connectionRequests = underTest.findConnectionRequests(userOne.id)
            assertThat(connectionRequests.initiatedConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }

        asUser(userTwo) {
            val connectionRequests = underTest.findConnectionRequests(userTwo.id)
            assertThat(connectionRequests.pendingConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }
    }

    private fun asUser(user: UserEntity, executable: (user: UserEntity) -> Unit) {
        val auth = UsernamePasswordAuthenticationToken(user.email, null, null)
        SecurityContextHolder.getContext().authentication = auth
        executable.invoke(user)
        SecurityContextHolder.getContext().authentication = null
    }
}