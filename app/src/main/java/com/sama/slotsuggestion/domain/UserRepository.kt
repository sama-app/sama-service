package com.sama.slotsuggestion.domain

import com.sama.users.domain.UserId

interface UserRepository {
    fun findById(userId: UserId): User
}