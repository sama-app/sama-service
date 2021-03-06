package com.sama.connection.application

import com.sama.common.BaseApplicationIntegrationTest
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.domain.UserAlreadyConnectedException
import com.sama.users.application.UserPublicDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.access.AccessDeniedException

class UserConnectionApplicationServiceIT : BaseApplicationIntegrationTest() {

    @Autowired
    lateinit var underTest: UserConnectionApplicationService

    @MockBean
    lateinit var commsEventConsumer: CommsEventConsumer

    @Test
    fun `connect two users via request`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(CreateConnectionRequestCommand(recipient().publicId))
        }

        verify(commsEventConsumer).onConnectionRequestCreated(anyOrNull())

        asRecipient {
            underTest.approveConnectionRequest(connectionRequest.connectionRequestId)
        }

        verify(commsEventConsumer).onUserConnected(anyOrNull())

        asInitiator {
            val connections = underTest.findUserConnections()
            val connectionRequests = underTest.findConnectionRequests()
            assertThat(connections.connectedUsers)
                .containsExactly(UserPublicDTO(recipient().publicId, recipient().fullName, recipient().email))
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections()
            val connectionRequests = underTest.findConnectionRequests()
            assertThat(connections.connectedUsers)
                .containsExactly(UserPublicDTO(initiator().publicId, initiator().fullName, initiator().email))
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }

        assertTrue { underTest.isConnected(initiator().id, recipient().id) }
    }

    @Test
    fun `connect two users directly`() {
        underTest.createUserConnection(initiator().id, CreateUserConnectionCommand(recipient().id), )

        assertThrows<UserAlreadyConnectedException> {
            underTest.createUserConnection(initiator().id, CreateUserConnectionCommand(recipient().id), )
        }

        assertTrue { underTest.isConnected(initiator().id, recipient().id) }

        verify(commsEventConsumer).onUserConnected(anyOrNull())
    }

    @Test
    fun `cannot manipulate own connection request`() {
        asInitiator {
            val connectionRequest =
                underTest.createConnectionRequest(CreateConnectionRequestCommand(recipient().publicId))

            assertThrows<AccessDeniedException> {
                underTest.approveConnectionRequest(connectionRequest.connectionRequestId)
            }

            assertThrows<AccessDeniedException> {
                underTest.rejectConnectionRequest(connectionRequest.connectionRequestId)
            }
        }
    }

    @Test
    fun `reject user connection request`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(CreateConnectionRequestCommand(recipient().publicId))
        }

        verify(commsEventConsumer).onConnectionRequestCreated(anyOrNull())

        asRecipient {
            underTest.rejectConnectionRequest(connectionRequest.connectionRequestId)
        }

        verify(commsEventConsumer).onConnectionRequestRejected(anyOrNull())

        asInitiator {
            val connections = underTest.findUserConnections()
            assertThat(connections.connectedUsers).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections()
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `disconnect users after connection`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(CreateConnectionRequestCommand(recipient().publicId))
        }

        asRecipient {
            underTest.approveConnectionRequest(connectionRequest.connectionRequestId)
        }

        asInitiator {
            underTest.removeUserConnection(RemoveUserConnectionCommand(recipient().publicId))
        }

        asInitiator {
            val connections = underTest.findUserConnections()
            assertThat(connections.connectedUsers).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections()
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `list connection requests for two users`() {
        val connectionRequestDTO = asInitiator {
            val request = underTest.createConnectionRequest(
                CreateConnectionRequestCommand(recipient().publicId)
            )
            ConnectionRequestDTO(
                request.connectionRequestId,
                initiator = UserPublicDTO(it.publicId, it.fullName, it.email),
                recipient = UserPublicDTO(recipient().publicId, recipient().fullName, recipient().email)
            )
        }

        asInitiator {
            val connectionRequests = underTest.findConnectionRequests()
            assertThat(connectionRequests.initiatedConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }

        asRecipient {
            val connectionRequests = underTest.findConnectionRequests()
            assertThat(connectionRequests.pendingConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }
    }
}