package com.sama.adapter.users

import com.sama.adapter.common.UserId
import com.sama.users.application.RegisterDeviceCommand
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserApplicationService
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
}