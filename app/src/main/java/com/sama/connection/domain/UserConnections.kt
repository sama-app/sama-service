package com.sama.connection.domain

import com.sama.connection.domain.ConnectionRequestStatus.APPROVED
import com.sama.connection.domain.ConnectionRequestStatus.PENDING
import com.sama.connection.domain.ConnectionRequestStatus.REJECTED
import com.sama.users.domain.UserId
import java.util.UUID

data class UserConnections(
    val userId: UserId,
    val connectedUsers: Set<UserId>,
    val discoveredUsers: Set<UserId>
) {

    fun allUserIds(): Set<UserId> {
        return connectedUsers.plus(discoveredUsers)
    }

    fun addDiscoveredUsers(newUsers: Collection<UserId>): UserConnections {
        return copy(discoveredUsers = discoveredUsers.plus(newUsers))
    }

    fun createConnectionRequest(recipientId: UserId): Pair<UserConnections, ConnectionRequest> {
        if (recipientId in connectedUsers) {
            throw UserAlreadyConnectedException(userId, recipientId)
        }

        return Pair(
            copy(discoveredUsers = discoveredUsers.plus(recipientId)),
            ConnectionRequest(UUID.randomUUID(), userId, recipientId, PENDING)
        )
    }

    fun approve(connectionRequest: ConnectionRequest): Pair<UserConnections, ConnectionRequest> {
        if (connectionRequest.isValid()) {
            throw InvalidConnectionRequest(connectionRequest.id)
        }
        if (connectionRequest.recipientId in connectedUsers) {
            throw UserAlreadyConnectedException(userId, connectionRequest.recipientId)
        }

        return Pair(
            copy(
                connectedUsers = connectedUsers.plus(connectionRequest.recipientId),
                discoveredUsers = discoveredUsers.minus(connectionRequest.recipientId)
            ),
            connectionRequest.copy(status = APPROVED)
        )
    }

    fun reject(connectionRequest: ConnectionRequest): Pair<UserConnections, ConnectionRequest> {
        if (connectionRequest.isValid()) {
            throw InvalidConnectionRequest(connectionRequest.id)
        }
        return Pair(this, connectionRequest.copy(status = REJECTED))
    }

    fun disconnect(userId: UserId): UserConnections {
        return copy(
            connectedUsers = connectedUsers.plus(userId),
            discoveredUsers = connectedUsers.minus(userId)
        )
    }
}