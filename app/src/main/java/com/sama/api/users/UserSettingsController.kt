package com.sama.api.users

import com.sama.api.config.AuthUserId
import com.sama.users.application.GrantUserPermissionsCommand
import com.sama.users.application.RevokeUserPermissionsCommand
import com.sama.users.application.UpdateMarketingPreferencesCommand
import com.sama.users.application.UpdateTimeZoneCommand
import com.sama.users.application.UpdateWorkingHoursCommand
import com.sama.users.application.UserSettingsDTO
import com.sama.users.application.UserSettingsService
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
class UserSettingsController(private val userSettingsService: UserSettingsService) {

    @Operation(
        summary = "Retrieve user settings",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/user/me/settings",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getSettings(@AuthUserId userId: UserId?): UserSettingsDTO =
        userSettingsService.find(userId!!)

    @Operation(
        summary = "Update user working hours",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-working-hours",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateWorkingHours(@AuthUserId userId: UserId?, @RequestBody command: UpdateWorkingHoursCommand) =
        userSettingsService.updateWorkingHours(userId!!, command)

    @Operation(
        summary = "Update user time zone",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-time-zone",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateTimeZone(@AuthUserId userId: UserId?, @RequestBody command: UpdateTimeZoneCommand) =
        userSettingsService.updateTimeZone(userId!!, command)

    @Operation(
        summary = "Grant Sama permissions to enable features",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/grant-permissions",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun grantPermissions(@AuthUserId userId: UserId?, @RequestBody command: GrantUserPermissionsCommand) =
        userSettingsService.grantPermissions(userId!!, command)

    @Operation(
        summary = "Revoke Sama permissions disabling some features",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/revoke-permissions",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun revokePermissions(@AuthUserId userId: UserId?, @RequestBody command: RevokeUserPermissionsCommand) =
        userSettingsService.revokePermissions(userId!!, command)

    @Operation(
        summary = "Update user marketing preferences",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/user/me/update-marketing-preferences",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun updateMarketingPreferences(@AuthUserId userId: UserId?, @RequestBody command: UpdateMarketingPreferencesCommand) =
        userSettingsService.updateMarketingPreferences(userId!!, command)
}