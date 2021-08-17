package com.sama.connection.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface ConnectionRequestRepository: Repository<ConnectionRequest, ConnectionRequestId> {
    fun findByIdOrThrow(connectionRequestId: ConnectionRequestId): ConnectionRequest
    fun findPendingByInitiatorId(userId: UserId): Collection<ConnectionRequest>
    fun findPendingByRecipientId(userId: UserId): Collection<ConnectionRequest>
    fun findPendingByUserIds(initiatorId: UserId, recipientId: UserId): ConnectionRequest?
    fun save(connectionRequest: ConnectionRequest)
}