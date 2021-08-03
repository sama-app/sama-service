package com.sama.connection.infrastructure

import com.sama.connection.domain.UserConnections
import com.sama.connection.domain.UserConnectionsRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class JdbcUserConnectionsRepository : UserConnectionsRepository {
    override fun findByIdOrThrow(userId: UserId): UserConnections {
        TODO("Not yet implemented")
    }

    override fun save(userConnections: UserConnections) {
        TODO("Not yet implemented")
    }
}