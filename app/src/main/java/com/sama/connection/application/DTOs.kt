package com.sama.connection.application

import com.sama.connection.domain.ConnectionRequestId
import java.util.UUID

data class UserConnectionsDTO(
    val connectedUsers: List<UserDTO>,
    val discoveredUsers: List<UserDTO>
)

data class UserDTO(val userId: UUID, val email: String, val fullName: String?)

data class ConnectionRequestsDTO(
    val initiatedConnectionRequests: List<ConnectionRequestDTO>,
    val pendingConnectionRequests: List<ConnectionRequestDTO>
)

data class ConnectionRequestDTO(
    val connectionRequestId: ConnectionRequestId,
    val initiator: UserDTO,
    val recipient: UserDTO
)