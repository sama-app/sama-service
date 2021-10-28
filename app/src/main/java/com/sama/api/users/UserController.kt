package com.sama.api.users

import com.sama.users.application.RegisterDeviceCommand
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserDeviceRegistrationService
import com.sama.users.application.UserService
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
    private val userApplicationService: UserService,
    private val userDeviceRegistrationService: UserDeviceRegistrationService
) {

    @Operation(
        summary = "Retrieve public user details",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/user/me/",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun me() = userApplicationService.me()

    @Operation(
        summary = "Register a device for push notifications via Firebase",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/register-device",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun registerDevice(@RequestBody command: RegisterDeviceCommand) =
        userDeviceRegistrationService.register(command)


    @Operation(
        summary = "Unregisters a device from receiving push notifications",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/unregister-device",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun unregisterDevice(@RequestBody command: UnregisterDeviceCommand) =
        userDeviceRegistrationService.unregister(command)
}