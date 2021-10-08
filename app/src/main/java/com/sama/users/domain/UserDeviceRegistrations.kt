package com.sama.users.domain

import com.sama.common.DomainEntity
import com.sama.common.NotFoundException
import com.sama.common.ValueObject
import java.util.UUID

@DomainEntity
data class UserDeviceRegistrations(val userId: UserId, val deviceRegistrations: Set<DeviceRegistration>) {
    val isRegistered = deviceRegistrations.isNotEmpty()

    fun register(deviceId: UUID, firebaseRegistrationToken: String): UserDeviceRegistrations {
        val deviceRegistration = DeviceRegistration(deviceId, firebaseRegistrationToken)
        return copy(deviceRegistrations = deviceRegistrations + deviceRegistration)

    }

    fun unregister(deviceId: UUID): UserDeviceRegistrations {
        val deviceRegistration = deviceRegistrations.find { it.deviceId == deviceId }
            ?: throw NotFoundException(UserDeviceRegistrations::class, deviceId)
        return copy(deviceRegistrations = deviceRegistrations - deviceRegistration)
    }
}

@ValueObject
data class DeviceRegistration(val deviceId: UUID, val firebaseRegistrationToken: String)