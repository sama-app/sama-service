package com.sama.users.application

import com.sama.users.domain.UserId

interface AuthUserService {
    fun currentUserIdOrNull(): UserId?
    fun currentUserOrNull(): UserInternalDTO?

    fun currentUserId(): UserId
    fun currentUser(): UserInternalDTO
}