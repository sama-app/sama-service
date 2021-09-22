package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.NotFoundException
import java.util.UUID

@DomainEntity
data class UserDeviceRegistrations(val userId: UserId, val deviceId: UUID?, val firebaseRegistrationToken: String?) {
    val isRegistered = deviceId != null && firebaseRegistrationToken != null

    fun register(deviceId: UUID, firebaseRegistrationToken: String): UserDeviceRegistrations {
        return copy(
            deviceId = deviceId,
            firebaseRegistrationToken = firebaseRegistrationToken
        )

    }

    fun unregister(deviceId: UUID): UserDeviceRegistrations {
        if (this.deviceId != deviceId) {
            throw NotFoundException(UserDeviceRegistrations::class, deviceId)
        }
        return copy(deviceId = null, firebaseRegistrationToken = null)

    }
}