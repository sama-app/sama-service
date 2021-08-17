package com.sama.connection.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface DiscoveredUserListRepository: Repository<DiscoveredUserList, UserId> {
    fun findById(userId: UserId): DiscoveredUserList

    fun save(discoveredUserList: DiscoveredUserList)
}