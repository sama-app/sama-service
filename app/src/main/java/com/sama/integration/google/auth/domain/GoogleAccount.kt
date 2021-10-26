package com.sama.integration.google.auth.domain

import com.sama.common.DomainEntity
import com.sama.users.domain.UserId
import java.util.UUID

// @JvmInline TODO: doesn't work with spring-data-jdbc
data class GoogleAccountId(val id: Long)

fun GoogleAccountId.toStorageKey() = id.toString()

@JvmInline
value class GoogleAccountPublicId(val id: UUID)

@DomainEntity
data class GoogleAccount(
    val id: GoogleAccountId?,
    val publicId: GoogleAccountPublicId?,
    val userId: UserId,
    val email: String,
    val primary: Boolean,
    val linked: Boolean
) {
    fun link(): GoogleAccount {
        return copy(linked = true)
    }

    fun unlink(): GoogleAccount {
        return copy(linked = false)
    }

    companion object {
        fun new(userId: UserId, email: String, primary: Boolean) =
            GoogleAccount(null, null, userId, email, primary, true)
    }
}