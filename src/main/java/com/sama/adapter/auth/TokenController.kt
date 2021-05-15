package com.sama.adapter.auth

import com.sama.users.application.RefreshTokenCommand
import com.sama.users.application.UserApplicationService
import com.sama.users.domain.JwtPair
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TokenController(
    private val userApplicationService: UserApplicationService
) {

    @PostMapping("/api/auth/refresh-token")
    fun refreshToken(@RequestBody command: RefreshTokenCommand): JwtPair {
        return userApplicationService.refreshToken(command)
    }
}