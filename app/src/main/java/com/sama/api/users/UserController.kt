package com.sama.api.users

import com.sama.api.config.AuthUserId
import com.sama.users.application.*
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "user")
@RestController
class UserController(
    private val userApplicationService: UserApplicationService
) {

    @Operation(
        summary = "Deletes the user from the system",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/delete"
    )
    fun deleteUser(@AuthUserId userId: UserId) =
        userApplicationService.deleteUser(userId)

    @Operation(
        summary = "Register a device for push notifications via Firebase",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/register-device",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun registerDevice(@AuthUserId userId: UserId, @RequestBody command: RegisterDeviceCommand) =
        userApplicationService.registerDevice(userId, command)


    @Operation(
        summary = "Unregisters a device from receiving push notifications",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/unregister-device",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun unregisterDevice(@AuthUserId userId: UserId, @RequestBody command: UnregisterDeviceCommand) =
        userApplicationService.unregisterDevice(userId, command)


    @Operation(
        summary = "Retrieve user settings",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/user/me/settings",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getSettings(@AuthUserId userId: UserId): UserSettingsDTO =
        userApplicationService.getUserSettings(userId)

    @Operation(
        summary = "Update user working hours",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-working-hours",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateWorkingHours(@AuthUserId userId: UserId, @RequestBody command: UpdateWorkingHoursCommand) =
        userApplicationService.updateWorkingHours(userId, command)

}