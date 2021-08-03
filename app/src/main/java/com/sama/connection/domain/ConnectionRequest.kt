package com.sama.connection.domain

import com.sama.users.domain.UserId

enum class ConnectionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ConnectionRequest(
    val id: ConnectionRequestId,
    val initiatorId: UserId,
    val recipientId: UserId,
    val status: ConnectionRequestStatus
) {

    fun isValid(): Boolean {
        return status == ConnectionRequestStatus.PENDING
    }
}