package com.sama.connection.infrastructure

import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class ConnectionRequestRepositoryImpl : ConnectionRequestRepository {
    override fun findByIdOrThrow(connectionRequestId: ConnectionRequestId): ConnectionRequest {
        TODO("Not yet implemented")
    }

    override fun existsPendingByUserIds(initiatorId: UserId, recipientId: UserId): Boolean {
        TODO("Not yet implemented")
    }

    override fun save(connectionRequest: ConnectionRequest) {
        TODO("Not yet implemented")
    }
}