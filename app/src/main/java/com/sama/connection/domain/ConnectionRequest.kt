package com.sama.connection.domain

import com.google.api.client.util.Preconditions
import com.sama.connection.domain.ConnectionRequestStatus.PENDING
import com.sama.users.domain.UserId
import java.util.UUID

enum class ConnectionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ConnectionRequest(
    val id: ConnectionRequestId,
    val initiatorUserId: UserId,
    val recipientUserId: UserId,
    val status: ConnectionRequestStatus,
) {

    companion object {
        fun new(initiatorId: UserId, recipientId: UserId): ConnectionRequest {
            check(recipientId != initiatorId) { "Cannot send connect request to oneself" }
            return ConnectionRequest(UUID.randomUUID(), initiatorId, recipientId, PENDING)
        }
    }

    private fun isValid(): Boolean {
        return status == PENDING
    }

    fun approve(): Pair<ConnectionRequest, UserConnection> {
        if (!isValid()) {
            throw InvalidConnectionRequest(id)
        }

        return Pair(
            copy(status = ConnectionRequestStatus.APPROVED),
            UserConnection(initiatorUserId, recipientUserId)
        )
    }

    fun reject(): ConnectionRequest {
        if (!isValid()) {
            throw InvalidConnectionRequest(id)
        }
        return copy(status = ConnectionRequestStatus.REJECTED)
    }

}