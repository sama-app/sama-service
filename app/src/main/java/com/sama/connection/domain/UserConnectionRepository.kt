package com.sama.connection.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface UserConnectionRepository : Repository<UserConnection, UserId> {
    fun findConnectedUserIds(userId: UserId): Collection<UserId>
    fun exists(userOneId: UserId, userTwoId: UserId): Boolean

    fun save(userConnection: UserConnection)
    fun delete(userConnection: UserConnection)
}