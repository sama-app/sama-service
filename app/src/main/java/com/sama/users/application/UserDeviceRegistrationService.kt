package com.sama.users.application

import com.sama.users.domain.UserId

interface UserDeviceRegistrationService {
    fun me(): UserDeviceRegistrationsDTO
    fun find(userId: UserId): UserDeviceRegistrationsDTO
    fun register(command: RegisterDeviceCommand): Boolean
    fun unregister(command: UnregisterDeviceCommand): Boolean
}