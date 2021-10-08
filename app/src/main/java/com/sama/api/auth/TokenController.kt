package com.sama.api.auth

import com.sama.users.application.RefreshTokenCommand
import com.sama.users.application.UserTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "auth")
@RestController
class TokenController(private val userTokenService: UserTokenService) {

    @Operation(summary = "Acquire a new authentication JWT pair using a refresh token")
    @PostMapping(
        "/api/auth/refresh-token",
        produces = [APPLICATION_JSON_VALUE],
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun refreshToken(@RequestBody command: RefreshTokenCommand) =
        userTokenService.refreshToken(command)
}