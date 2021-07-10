package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.Factory

@DomainEntity
data class BasicUserDetails(
    val email: String,
    val fullName: String?
) {

    @Factory
    companion object {
        fun of(user: UserEntity): BasicUserDetails {
            return BasicUserDetails(
                user.email,
                user.fullName
            )
        }
    }

    fun rename(fullName: String?): BasicUserDetails {
        return copy(fullName = fullName)
    }
}
