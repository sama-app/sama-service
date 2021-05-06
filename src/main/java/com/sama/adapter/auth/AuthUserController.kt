package com.sama.adapter.auth

import com.sama.auth.application.AuthUserApplicationService
import com.sama.auth.application.RefreshTokenCommand
import com.sama.auth.application.RegisterDeviceCommand
import com.sama.auth.application.UnregisterDeviceCommand
import com.sama.auth.domain.JwtPair
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthUserController(
    private val authUserApplicationService: AuthUserApplicationService
) {

    @PostMapping("/api/auth/user/register-device")
    fun registerDevice(@UserId userId: Long, @RequestBody command: RegisterDeviceCommand): Boolean {
        return authUserApplicationService.registerDevice(userId, command)
    }

    @PostMapping("/api/auth/user/unregister-device")
    fun unregisterDevice(@UserId userId: Long, @RequestBody command: UnregisterDeviceCommand): Boolean {
        return authUserApplicationService.unregisterDevice(userId, command)
    }

    @PostMapping("/api/auth/user/refresh-token")
    fun refreshToken(@UserId userId: Long, @RequestBody command: RefreshTokenCommand): JwtPair {
        return authUserApplicationService.refreshToken(userId, command)
    }
}