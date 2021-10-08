package com.sama.connection.application

import com.fasterxml.jackson.annotation.JsonCreator
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import java.util.UUID.fromString

data class CreateUserConnectionCommand(val recipientId: UserId)
data class AddDiscoveredUsersCommand(val userEmails: Set<String>)

data class CreateConnectionRequestCommand(val recipientId: UserPublicId) {
    @JsonCreator // Jackson cannot de-serialize value classes just yet
    private constructor(recipientId: String) : this(UserPublicId(fromString(recipientId)))
}

data class RemoveUserConnectionCommand(val userId: UserPublicId) {
    @JsonCreator // Jackson cannot de-serialize value classes just yet
    private constructor(userId: String) : this(UserPublicId(fromString(userId)))
}
