package com.sama.connection.domain

import com.sama.users.domain.UserId

interface UserConnectionsRepository {
    fun findByIdOrThrow(userId: UserId): UserConnections

    fun save(userConnections: UserConnections)
}