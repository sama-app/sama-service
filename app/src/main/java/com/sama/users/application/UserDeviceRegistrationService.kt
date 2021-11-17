package com.sama.users.application

import com.sama.common.InternalApi
import com.sama.users.domain.UserId

interface UserDeviceRegistrationService {
    fun me(): UserDeviceRegistrationsDTO
    fun find(userId: UserId): UserDeviceRegistrationsDTO
    fun register(command: RegisterDeviceCommand): Boolean
    @InternalApi
    fun unregister(userId: UserId, command: UnregisterDeviceCommand): Boolean
    fun unregister(command: UnregisterDeviceCommand): Boolean
}