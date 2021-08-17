package com.sama.slotsuggestion.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId

@DomainRepository
interface UserRepository {
    fun findById(userId: UserId): User
}