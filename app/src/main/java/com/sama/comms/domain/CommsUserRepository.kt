package com.sama.comms.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId

@DomainRepository
interface CommsUserRepository {
    fun findById(userId: UserId): CommsUser
}