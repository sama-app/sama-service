package com.sama.api.users

import com.sama.api.common.UserId
import com.sama.common.NotFoundException
import com.sama.users.application.RegisterDeviceCommand
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.application.UserSettingsDTO
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class UserController(
    private val userApplicationService: UserApplicationService
) {

    @PostMapping("/api/user/register-device")
    fun registerDevice(@UserId userId: Long, @RequestBody command: RegisterDeviceCommand): Boolean {
        return userApplicationService.registerDevice(userId, command)
    }

    @PostMapping("/api/user/unregister-device")
    fun unregisterDevice(@UserId userId: Long, @RequestBody command: UnregisterDeviceCommand): Boolean {
        return userApplicationService.unregisterDevice(userId, command)
    }

    @GetMapping("/api/user/settings")
    fun getSettings(@UserId userId: Long): UserSettingsDTO {
        return userApplicationService.getSettings(userId)
    }
}