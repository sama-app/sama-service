package com.sama.users.application

import com.sama.common.ApplicationService
import com.sama.users.configuration.AccessJwtConfiguration
import com.sama.users.configuration.RefreshJwtConfiguration
import com.sama.users.domain.InvalidRefreshTokenException
import com.sama.users.domain.Jwt
import com.sama.users.domain.UserId
import com.sama.users.domain.UserJwtIssuer
import com.sama.users.domain.UserRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service

@ApplicationService
@Service
class UserTokenApplicationService(
    private val userRepository: UserRepository,
    private val accessJwtConfiguration: AccessJwtConfiguration,
    private val refreshJwtConfiguration: RefreshJwtConfiguration,
    private val clock: Clock,
) : UserTokenService {

    override fun issueTokens(userId: UserId): JwtPairDTO {
        val userDetails = userRepository.findByIdOrThrow(userId)
        val userJwtIssuer = UserJwtIssuer(userDetails)
        val refreshJwt = userJwtIssuer.issue(UUID.randomUUID(), refreshJwtConfiguration, clock)
            .getOrThrow()
        val accessJwt = userJwtIssuer.issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessJwt.token, refreshJwt.token)
    }

    override fun refreshToken(command: RefreshTokenCommand): JwtPairDTO {
        val refreshToken = Jwt.verified(command.refreshToken, refreshJwtConfiguration, clock)
            .onFailure { throw InvalidRefreshTokenException() }
            .getOrThrow()

        val userDetails = if (refreshToken.userId() != null) {
            userRepository.findByPublicIdOrThrow(refreshToken.userId()!!)
        } else {
            userRepository.findByEmailOrThrow(refreshToken.userEmail())
        }

        val accessToken = UserJwtIssuer(userDetails)
            .issue(UUID.randomUUID(), accessJwtConfiguration, clock)
            .getOrThrow()

        return JwtPairDTO(accessToken.token, refreshToken.token)
    }
}