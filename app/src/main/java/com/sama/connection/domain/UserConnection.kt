package com.sama.connection.domain

import com.google.api.client.util.Preconditions.checkArgument
import com.sama.users.domain.UserId

data class UserConnection(val leftUserId: UserId, val rightUserId: UserId) {
    init {
        checkArgument(leftUserId != rightUserId)
    }
}