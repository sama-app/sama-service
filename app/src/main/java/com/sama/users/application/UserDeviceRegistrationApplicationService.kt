package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserDeviceRegistrationApplicationService(private val userRepository: UserRepository) :
    UserDeviceRegistrationService {

    @Transactional(readOnly = true)
    override fun findByUserId(userId: UserId): UserDeviceRegistrationsDTO {
        return userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .deviceRegistrations
            .map { FirebaseDeviceRegistrationDTO(it.deviceId, it.firebaseRegistrationToken) }
            .let { UserDeviceRegistrationsDTO(it) }
    }

    @Transactional
    override fun register(userId: UserId, command: RegisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .register(command.deviceId, command.firebaseRegistrationToken)
        userRepository.save(changes)
        return true
    }

    @Transactional
    override fun unregister(userId: UserId, command: UnregisterDeviceCommand): Boolean {
        val changes = userRepository.findDeviceRegistrationsByIdOrThrow(userId)
            .unregister(command.deviceId)
        userRepository.save(changes)
        return true
    }
}