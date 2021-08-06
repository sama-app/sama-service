package com.sama.users.domain

import com.sama.common.Factory

data class UserPublicDetails(
    val id: UserId?,
    val publicId: UserPublicId?,
    val email: String,
    val fullName: String?
) {

    @Factory
    companion object {
        fun of(user: UserEntity): UserPublicDetails {
            return UserPublicDetails(
                user.id,
                user.publicId,
                user.email,
                user.fullName
            )
        }
    }

    fun rename(fullName: String?): UserPublicDetails {
        return copy(fullName = fullName)
    }
}
