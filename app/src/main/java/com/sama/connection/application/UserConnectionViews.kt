package com.sama.connection.application

import com.sama.connection.domain.ConnectionRequest
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class UserConnectionViews(private val userService: UserService) {

    fun renderUserConnections(discoveredUsers: Set<UserId>, connectedUsers: Collection<UserId>): UserConnectionsDTO {
        val allUserIds = discoveredUsers.plus(connectedUsers)
        val userIdToPublicDetails = userService.findAll(allUserIds)

        return UserConnectionsDTO(
            connectedUsers.mapNotNull { connectionUserId ->
                userIdToPublicDetails[connectionUserId]
            },
            discoveredUsers.mapNotNull { connectionUserId ->
                userIdToPublicDetails[connectionUserId]
            },
        )
    }

    fun renderConnectionRequests(
        initiatedConnectionRequests: Collection<ConnectionRequest>,
        pendingConnectionRequests: Collection<ConnectionRequest>
    ): ConnectionRequestsDTO {

        return ConnectionRequestsDTO(
            initiatedConnectionRequests.map { it.toDTO() },
            pendingConnectionRequests.map { it.toDTO() }
        )
    }

    fun renderConnectionRequest(connectionRequest: ConnectionRequest): ConnectionRequestDTO {
        return connectionRequest.toDTO()
    }

    private fun ConnectionRequest.toDTO(): ConnectionRequestDTO {
        val userIdToPublicDetails = userService.findAll(setOf(this.initiatorUserId, this.recipientUserId))

        return ConnectionRequestDTO(
            id,
            initiator = userIdToPublicDetails[initiatorUserId]!!,
            recipient = userIdToPublicDetails[recipientUserId]!!
        )
    }
}