package com.sama.users.domain

import java.util.UUID

@JvmInline
value class UserId(val id: Long)

@JvmInline
value class UserPublicId(val id: UUID) {
    companion object {
        fun of(string: String): UserPublicId {
            return UserPublicId(UUID.fromString(string))
        }

        fun random(): UserPublicId {
            return UserPublicId(UUID.randomUUID())
        }
    }
}