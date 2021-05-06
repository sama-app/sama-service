package com.sama.auth.application

import com.sama.auth.configuration.AccessJwtConfiguration
import com.sama.auth.configuration.RefreshJwtConfiguration
import com.sama.auth.domain.AuthUserRepository
import com.sama.auth.domain.JwtPair
import com.sama.common.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.*

@Service
class AuthUserApplicationService(
    private val authUserRepository: AuthUserRepository,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock
) {

    fun registerDevice(userId: Long, command: RegisterDeviceCommand): Boolean {
        val user = authUserRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        user.registerFirebaseDevice(command.deviceId, command.firebaseRegistrationToken)
        authUserRepository.save(user)
        return true
    }

    fun unregisterDevice(userId: Long, command: UnregisterDeviceCommand): Boolean {
        val user = authUserRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        user.unregisterFirebaseDevice(command.deviceId)
        authUserRepository.save(user)
        return true
    }

    fun refreshToken(userId: Long, command: RefreshTokenCommand): JwtPair {
        val user = authUserRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        return user.refreshJwt(command.refreshToken, accessJwtConfiguration, refreshJwtConfiguration, clock)
    }
}

data class RegisterDeviceCommand(val deviceId: UUID, val firebaseRegistrationToken: String)
data class UnregisterDeviceCommand(val deviceId: UUID)
data class RefreshTokenCommand(val refreshToken: String)