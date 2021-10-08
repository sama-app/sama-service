package com.sama.users.application

import com.sama.users.domain.UserId

interface UserTokenService {
    fun issueTokens(userId: UserId): JwtPairDTO
    fun refreshToken(command: RefreshTokenCommand): JwtPairDTO
}