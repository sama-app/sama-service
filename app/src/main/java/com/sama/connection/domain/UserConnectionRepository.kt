package com.sama.connection.domain

import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

interface UserConnectionRepository : Repository<UserConnection, UserId> {
    fun findConnectedUserIds(userId: UserId): Collection<UserId>

    fun save(userConnection: UserConnection)

    fun delete(userConnection: UserConnection)
}