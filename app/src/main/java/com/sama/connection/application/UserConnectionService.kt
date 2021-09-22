package com.sama.connection.application

import com.sama.common.InternalApi
import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.domain.UserId

interface UserConnectionService {
    fun isConnected(userOneId: UserId, userTwoId: UserId): Boolean
    fun findUserConnections(userId: UserId): UserConnectionsDTO
    @InternalApi
    fun createUserConnection(userId: UserId, command: CreateUserConnectionCommand): Boolean
    fun removeUserConnection(userId: UserId, command: RemoveUserConnectionCommand)
    @InternalApi
    fun addDiscoveredUsers(userId: UserId, command: AddDiscoveredUsersCommand): Boolean

    fun findConnectionRequests(userId: UserId): ConnectionRequestsDTO
    fun createConnectionRequest(userId: UserId, command: CreateConnectionRequestCommand): ConnectionRequestDTO
    fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId)
    fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId)
}