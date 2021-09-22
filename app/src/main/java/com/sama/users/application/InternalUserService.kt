package com.sama.users.application

import com.sama.common.InternalApi
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId

interface InternalUserService: UserService {
    @InternalApi
    fun translatePublicId(userPublicId: UserPublicId): UserId

    @InternalApi
    fun findIdsByEmail(emails: Set<String>): Set<UserId>

    @InternalApi
    fun findInternalByEmail(email: String): UserInternalDTO

    @InternalApi
    fun findInternalByPublicId(userPublicId: UserPublicId): UserInternalDTO
}