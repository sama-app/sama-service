package com.sama.connection.application

import com.sama.common.BaseApplicationTest
import com.sama.users.application.UserPublicDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class UserConnectionApplicationServiceTest : BaseApplicationTest() {

    @Autowired
    lateinit var underTest: UserConnectionApplicationService

    @Test
    fun `connect two users via request`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(it.id!!, CreateConnectionRequestCommand(recipient().publicId!!))
        }

        asRecipient {
            underTest.approveConnectionRequest(it.id!!, connectionRequest.connectionRequestId)
        }

        asInitiator {
            val connections = underTest.findUserConnections(it.id!!)
            val connectionRequests = underTest.findConnectionRequests(it.id!!)
            assertThat(connections.connectedUsers)
                .containsExactly(UserPublicDTO(recipient().publicId!!, recipient().fullName, recipient().email))
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections(it.id!!)
            val connectionRequests = underTest.findConnectionRequests(it.id!!)
            assertThat(connections.connectedUsers)
                .containsExactly(UserPublicDTO(initiator().publicId!!, initiator().fullName, initiator().email))
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }
    }

    @Test
    fun `cannot manipulate own connection request`() {
        asInitiator {
            val connectionRequest =
                underTest.createConnectionRequest(it.id!!, CreateConnectionRequestCommand(recipient().publicId!!))

            assertThrows<AccessDeniedException> {
                underTest.approveConnectionRequest(it.id!!, connectionRequest.connectionRequestId)
            }

            assertThrows<AccessDeniedException> {
                underTest.rejectConnectionRequest(it.id!!, connectionRequest.connectionRequestId)
            }
        }
    }

    @Test
    fun `reject user connection request`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(it.id!!, CreateConnectionRequestCommand(recipient().publicId!!))
        }

        asRecipient {
            underTest.rejectConnectionRequest(recipient().id!!, connectionRequest.connectionRequestId)
        }

        asInitiator {
            val connections = underTest.findUserConnections(it.id!!)
            assertThat(connections.connectedUsers).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections(it.id!!)
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `disconnect users after connection`() {
        val connectionRequest = asInitiator {
            underTest.createConnectionRequest(it.id!!, CreateConnectionRequestCommand(recipient().publicId!!))
        }

        asRecipient {
            underTest.approveConnectionRequest(it.id!!, connectionRequest.connectionRequestId)
        }

        asInitiator {
            underTest.removeUserConnection(it.id!!, RemoveUserConnectionCommand(recipient().publicId!!))
        }

        asInitiator {
            val connections = underTest.findUserConnections(it.id!!)
            assertThat(connections.connectedUsers).isEmpty()
        }

        asRecipient {
            val connections = underTest.findUserConnections(recipient().id!!)
            assertThat(connections.connectedUsers).isEmpty()
        }
    }

    @Test
    fun `list connection requests for two users`() {
        val connectionRequestDTO = asInitiator {
            val request = underTest.createConnectionRequest(
                it.id!!,
                CreateConnectionRequestCommand(recipient().publicId!!)
            )
            ConnectionRequestDTO(
                request.connectionRequestId,
                initiator = UserPublicDTO(it.publicId!!, it.fullName, it.email),
                recipient = UserPublicDTO(recipient().publicId!!, recipient().fullName, recipient().email)
            )
        }

        asInitiator {
            val connectionRequests = underTest.findConnectionRequests(it.id!!)
            assertThat(connectionRequests.initiatedConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.pendingConnectionRequests).isEmpty()
        }

        asRecipient {
            val connectionRequests = underTest.findConnectionRequests(it.id!!)
            assertThat(connectionRequests.pendingConnectionRequests)
                .containsExactly(connectionRequestDTO)
            assertThat(connectionRequests.initiatedConnectionRequests).isEmpty()
        }
    }
}