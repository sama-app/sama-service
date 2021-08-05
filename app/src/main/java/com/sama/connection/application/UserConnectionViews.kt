package com.sama.connection.application

import com.sama.connection.domain.ConnectionRequest
import com.sama.users.domain.BasicUserDetails
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Component

@Component
class UserConnectionViews(private val userRepository: UserRepository) {

    fun renderUserConnections(discoveredUsers: Set<UserId>, connectedUsers: Collection<UserId>): UserConnectionsDTO {
        val allUserIds = discoveredUsers.plus(connectedUsers)
        val userIdToBasicDetails = userRepository.findBasicDetailsById(allUserIds)
            .associateBy { it.id }

        return UserConnectionsDTO(
            connectedUsers.mapNotNull { connectionUserId ->
                userIdToBasicDetails[connectionUserId]?.toUserDTO()
            },
            discoveredUsers.mapNotNull { connectionUserId ->
                userIdToBasicDetails[connectionUserId]?.toUserDTO()
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
        val userDetails = userRepository.findBasicDetailsById(setOf(this.initiatorUserId, this.recipientUserId))
            .associateBy { it.id }

        return ConnectionRequestDTO(
            id,
            initiator = userDetails[initiatorUserId]!!.toUserDTO(),
            recipient = userDetails[recipientUserId]!!.toUserDTO()
        )
    }

    private fun BasicUserDetails.toUserDTO(): UserDTO {
        return UserDTO(this.email, this.fullName)
    }
}