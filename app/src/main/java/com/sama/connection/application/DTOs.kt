package com.sama.connection.application

import com.sama.users.domain.BasicUserDetails

data class UserConnectionsDTO(
    val connectedUsers: List<UserConnectionDTO>,
    val discoveredUsers: List<UserConnectionDTO>
)

data class UserConnectionDTO(val email: String, val fullName: String?)

fun BasicUserDetails.toUserConnectionDTO(): UserConnectionDTO {
    return UserConnectionDTO(this.email, this.fullName)
}