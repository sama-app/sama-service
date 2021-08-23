package com.sama.api.users

import com.sama.api.config.AuthUserId
import com.sama.users.application.UpdateTimeZoneCommand
import com.sama.users.application.UpdateWorkingHoursCommand
import com.sama.users.application.UserSettingsApplicationService
import com.sama.users.application.UserSettingsDTO
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
class UserSettingsController(
    private val userSettingsApplicationService: UserSettingsApplicationService
) {

    @Operation(
        summary = "Retrieve user settings",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/user/me/settings",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getSettings(@AuthUserId userId: UserId?): UserSettingsDTO =
        userSettingsApplicationService.find(userId!!)

    @Operation(
        summary = "Update user working hours",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-working-hours",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateWorkingHours(@AuthUserId userId: UserId?, @RequestBody command: UpdateWorkingHoursCommand) =
        userSettingsApplicationService.updateWorkingHours(userId!!, command)


    @Operation(
        summary = "Update user time zone",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-time-zone",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateTimeZone(@AuthUserId userId: UserId?, @RequestBody command: UpdateTimeZoneCommand) =
        userSettingsApplicationService.updateTimeZone(userId!!, command)
}