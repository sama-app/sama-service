package com.sama.connection.application

import com.sama.users.domain.UserPublicId

data class CreateConnectionRequestCommand(val recipientId: UserPublicId)
data class RemoveUserConnectionCommand(val userId: UserPublicId)