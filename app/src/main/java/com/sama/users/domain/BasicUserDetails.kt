package com.sama.users.domain

import com.sama.common.Factory

data class BasicUserDetails(
    val id: UserId?,
    val email: String,
    val fullName: String?
) {

    @Factory
    companion object {
        fun of(user: UserEntity): BasicUserDetails {
            return BasicUserDetails(
                user.id,
                user.email,
                user.fullName
            )
        }
    }

    fun rename(fullName: String?): BasicUserDetails {
        return copy(fullName = fullName)
    }
}
