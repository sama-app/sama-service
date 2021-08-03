package com.sama.connection.domain

import com.sama.users.domain.UserId

interface ConnectionRequestRepository {
    fun findByIdOrThrow(connectionRequestId: ConnectionRequestId): ConnectionRequest

    fun existsPendingByUserIds(initiatorId: UserId, recipientId: UserId): Boolean

    fun save(connectionRequest: ConnectionRequest)
}