package com.sama.connection.application

import com.sama.connection.domain.ConnectionRequest
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicDetails
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Component

@Component
class UserConnectionViews(private val userRepository: UserRepository) {

    fun renderUserConnections(discoveredUsers: Set<UserId>, connectedUsers: Collection<UserId>): UserConnectionsDTO {
        val allUserIds = discoveredUsers.plus(connectedUsers)
        val userIdToPublicDetails = userRepository.findPublicDetailsById(allUserIds)
            .associateBy { it.id }

        return UserConnectionsDTO(
            connectedUsers.mapNotNull { connectionUserId ->
                userIdToPublicDetails[connectionUserId]?.toUserDTO()
            },
            discoveredUsers.mapNotNull { connectionUserId ->
                userIdToPublicDetails[connectionUserId]?.toUserDTO()
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
        val userDetails = userRepository.findPublicDetailsById(setOf(this.initiatorUserId, this.recipientUserId))
            .associateBy { it.id }

        return ConnectionRequestDTO(
            id,
            initiator = userDetails[initiatorUserId]!!.toUserDTO(),
            recipient = userDetails[recipientUserId]!!.toUserDTO()
        )
    }

    private fun UserPublicDetails.toUserDTO(): UserDTO {
        return UserDTO(publicId!!, email, fullName)
    }
}