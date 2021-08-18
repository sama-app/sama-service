package com.sama.users.application

import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId

interface InternalUserService: UserService {
    fun translatePublicId(userPublicId: UserPublicId): UserId
    fun findInternalByEmail(email: String): UserInternalDTO
    fun findInternalByPublicId(userPublicId: UserPublicId): UserInternalDTO
}