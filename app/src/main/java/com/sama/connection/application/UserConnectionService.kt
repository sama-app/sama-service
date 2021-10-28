package com.sama.connection.application

import com.sama.common.InternalApi
import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.domain.UserId

interface UserConnectionService {
    @InternalApi
    fun isConnected(userOneId: UserId, userTwoId: UserId): Boolean
    fun findUserConnections(): UserConnectionsDTO

    @InternalApi
    fun createUserConnection(userId: UserId, command: CreateUserConnectionCommand): Boolean
    fun removeUserConnection(command: RemoveUserConnectionCommand): Boolean

    @InternalApi
    fun addDiscoveredUsers(userId: UserId, command: AddDiscoveredUsersCommand): Boolean

    fun findConnectionRequests(): ConnectionRequestsDTO
    fun createConnectionRequest(command: CreateConnectionRequestCommand): ConnectionRequestDTO
    fun approveConnectionRequest(connectionRequestId: ConnectionRequestId): Boolean
    fun rejectConnectionRequest(connectionRequestId: ConnectionRequestId): Boolean
}