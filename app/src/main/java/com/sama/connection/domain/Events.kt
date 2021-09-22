package com.sama.connection.domain

import com.sama.common.DomainEvent
import com.sama.users.domain.UserId

@DomainEvent
data class UserConnectionRequestCreatedEvent(val actorId: UserId, val connectionRequest: ConnectionRequest)

@DomainEvent
data class UserConnectionRequestRejectedEvent(val actorId: UserId, val connectionRequest: ConnectionRequest)

@DomainEvent
data class UserConnectedEvent(val actorId: UserId, val userConnection: UserConnection)
