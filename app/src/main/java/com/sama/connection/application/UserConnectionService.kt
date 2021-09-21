package com.sama.connection.application

import com.sama.common.InternalApi
import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.domain.UserId

interface UserConnectionService {
    fun isConnected(userOneId: UserId, userTwoId: UserId): Boolean
    fun findUserConnections(userId: UserId): UserConnectionsDTO
    @InternalApi
    fun createUserConnection(createConnectionCommand: CreateConnectionCommand): Boolean
    fun removeUserConnection(userId: UserId, command: RemoveUserConnectionCommand)

    fun findConnectionRequests(userId: UserId): ConnectionRequestsDTO
    fun createConnectionRequest(initiatorId: UserId, command: CreateConnectionRequestCommand): ConnectionRequestDTO
    fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId)
    fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId)
}