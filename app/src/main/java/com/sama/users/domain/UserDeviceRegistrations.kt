package com.sama.users.domain

import com.sama.common.DomainEntity
import java.util.UUID

@DomainEntity
data class UserDeviceRegistrations(val userId: UserId, val deviceId: UUID?, val firebaseRegistrationToken: String?) {
    fun register(deviceId: UUID, firebaseRegistrationToken: String): Result<UserDeviceRegistrations> {
        return Result.success(
            copy(
                deviceId = deviceId,
                firebaseRegistrationToken = firebaseRegistrationToken
            )
        )
    }

    fun unregister(deviceId: UUID): Result<UserDeviceRegistrations> {
        return Result.success(
            copy(deviceId = null, firebaseRegistrationToken = null)
        )
    }
}