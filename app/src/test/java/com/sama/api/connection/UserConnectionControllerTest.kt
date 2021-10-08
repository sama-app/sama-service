package com.sama.api.connection

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.connection.application.ConnectionRequestDTO
import com.sama.connection.application.ConnectionRequestsDTO
import com.sama.connection.application.CreateConnectionRequestCommand
import com.sama.connection.application.RemoveUserConnectionCommand
import com.sama.connection.application.UserConnectionService
import com.sama.connection.application.UserConnectionsDTO
import com.sama.users.application.UserPublicDTO
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import java.util.UUID.randomUUID
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        UserConnectionController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class UserConnectionControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockBean
    lateinit var userConnectionService: UserConnectionService

    private val userId = UserId(1)
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"


    private val userInitiator = UserPublicDTO(UserPublicId(randomUUID()), "Initiator", "initiator@meetsama.com")
    private val userOne = UserPublicDTO(UserPublicId(randomUUID()), "Recipient", "recipient@meetsama.com")
    private val userTwo = UserPublicDTO(UserPublicId(randomUUID()), "Recipient2", "recipient2@meetsama.com")

    @Test
    fun `find user connections`() {
        whenever(userConnectionService.findUserConnections(userId))
            .thenReturn(
                UserConnectionsDTO(
                    connectedUsers = listOf(userOne),
                    discoveredUsers = listOf(userTwo)
                )
            )

        val expected = """
            {
                "connectedUsers": [
                    {
                        "userId": "${userOne.userId.id}",
                        "fullName": "${userOne.fullName}",
                        "email": "${userOne.email}"
                    }
                ],
                "discoveredUsers": [
                    {
                        "userId": "${userTwo.userId.id}",
                        "fullName": "${userTwo.fullName}",
                        "email": "${userTwo.email}"
                    }
                ]
            }
        """

        mockMvc.perform(
            get("/api/connection/user-connections")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }

    @Test
    fun `disconnect user`() {
        val targetUserId = UserPublicId(randomUUID())
        whenever(userConnectionService.removeUserConnection(userId, RemoveUserConnectionCommand(targetUserId)))
            .thenReturn(true)

        val payload = """
            {
                "userId": "${targetUserId.id}"
            }
        """

        mockMvc.perform(
            post("/api/connection/user-connections/disconnect-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `find connection requests`() {
        val initiatedRequest = ConnectionRequestDTO(randomUUID(), initiator = userInitiator, recipient = userOne)
        val pendingRequest = ConnectionRequestDTO(randomUUID(), initiator = userTwo, recipient = userInitiator)

        whenever(userConnectionService.findConnectionRequests(userId))
            .thenReturn(
                ConnectionRequestsDTO(
                    initiatedConnectionRequests = listOf(initiatedRequest),
                    pendingConnectionRequests = listOf(pendingRequest)
                )
            )

        val expected = """
            {
                "initiatedConnectionRequests": [
                    {
                        "connectionRequestId": "${initiatedRequest.connectionRequestId}",
                        "initiator": {
                            "userId": "${initiatedRequest.initiator.userId.id}",
                            "fullName": "${initiatedRequest.initiator.fullName}",
                            "email": "${initiatedRequest.initiator.email}"
                        },
                        "recipient": {
                            "userId": "${initiatedRequest.recipient.userId.id}",
                            "fullName": "${initiatedRequest.recipient.fullName}",
                            "email": "${initiatedRequest.recipient.email}"     
                        }
                    }
                ],
                "pendingConnectionRequests": [
                    {
                        "connectionRequestId": "${pendingRequest.connectionRequestId}",
                        "initiator": {
                            "userId": "${pendingRequest.initiator.userId.id}",
                            "fullName": "${pendingRequest.initiator.fullName}",
                            "email": "${pendingRequest.initiator.email}"
                        },
                        "recipient": {
                            "userId": "${pendingRequest.recipient.userId.id}",
                            "fullName": "${pendingRequest.recipient.fullName}",
                            "email": "${pendingRequest.recipient.email}"     
                        }
                    }
                ]
            }
        """

        mockMvc.perform(
            get("/api/connection/requests")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }

    @Test
    fun `create connection request`() {
        val targetUserId = userOne.userId
        val connectionRequest = ConnectionRequestDTO(randomUUID(), initiator = userInitiator, recipient = userOne)
        whenever(userConnectionService.createConnectionRequest(userId, CreateConnectionRequestCommand(targetUserId)))
            .thenReturn(connectionRequest)

        val payload = """
            {
                "recipientId": "${targetUserId.id}"
            }
        """

        val expected = """
            {
                "connectionRequestId": "${connectionRequest.connectionRequestId}",
                "initiator": {
                    "userId": "${connectionRequest.initiator.userId.id}",
                    "fullName": "${connectionRequest.initiator.fullName}",
                    "email": "${connectionRequest.initiator.email}"
                },
                "recipient": {
                    "userId": "${connectionRequest.recipient.userId.id}",
                    "fullName": "${connectionRequest.recipient.fullName}",
                    "email": "${connectionRequest.recipient.email}"     
                }
            }
        """

        mockMvc.perform(
            post("/api/connection/request/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expected))
    }

    @Test
    fun `approve connection request`() {
        val connectionRequestId = randomUUID()
        whenever(userConnectionService.approveConnectionRequest(userId, connectionRequestId))
            .thenReturn(true)

        mockMvc.perform(
            post("/api/connection/request/${connectionRequestId}/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `reject connection request`() {
        val connectionRequestId = randomUUID()
        whenever(userConnectionService.rejectConnectionRequest(userId, connectionRequestId))
            .thenReturn(true)

        mockMvc.perform(
            post("/api/connection/request/${connectionRequestId}/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }


    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        get("/api/connection/user-connections") to UNAUTHORIZED,
        post("/api/connection/user-connections/disconnect-user") to UNAUTHORIZED,
        get("/api/connection/requests") to UNAUTHORIZED,
        post("/api/connection/request/create") to UNAUTHORIZED,
        post("/api/connection/request/${randomUUID()}/accept") to UNAUTHORIZED,
        post("/api/connection/request/${randomUUID()}/reject") to UNAUTHORIZED,
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(MockMvcResultMatchers.status().isEqualTo(expectedStatus.value()))
            }
        }
}