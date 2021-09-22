package com.sama.connection.application

import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId

data class CreateUserConnectionCommand(val recipientId: UserId)
data class CreateConnectionRequestCommand(val recipientId: UserPublicId)
data class RemoveUserConnectionCommand(val userId: UserPublicId)