package com.sama.comms.domain

import com.sama.users.domain.UserId

interface CommsUserRepository {
    fun findById(userId: UserId): CommsUser
}