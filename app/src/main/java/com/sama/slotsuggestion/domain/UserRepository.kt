package com.sama.slotsuggestion.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface UserRepository: Repository<User, UserId> {
    fun findById(userId: UserId): User
}