package com.sama.api.users

import com.sama.api.common.UserId
import com.sama.users.application.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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

    @PostMapping("/api/user/update-working-hours")
    fun updateWorkingHours(@UserId userId: Long, @RequestBody command: UpdateWorkingHoursCommand): Boolean {
        return userApplicationService.updateWorkingHours(userId, command)
    }
}