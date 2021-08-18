package com.sama.connection.application

import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.application.UserPublicDTO
import java.util.UUID

data class UserConnectionsDTO(
    val connectedUsers: List<UserPublicDTO>,
    val discoveredUsers: List<UserPublicDTO>
)

data class ConnectionRequestsDTO(
    val initiatedConnectionRequests: List<ConnectionRequestDTO>,
    val pendingConnectionRequests: List<ConnectionRequestDTO>
)

data class ConnectionRequestDTO(
    val connectionRequestId: ConnectionRequestId,
    val initiator: UserPublicDTO,
    val recipient: UserPublicDTO
)