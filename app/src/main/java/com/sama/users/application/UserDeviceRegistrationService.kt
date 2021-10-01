package com.sama.users.application

import com.sama.users.domain.UserId

interface UserDeviceRegistrationService {
    fun findByUserId(userId: UserId): UserDeviceRegistrationsDTO
    fun register(userId: UserId, command: RegisterDeviceCommand): Boolean
    fun unregister(userId: UserId, command: UnregisterDeviceCommand): Boolean
}