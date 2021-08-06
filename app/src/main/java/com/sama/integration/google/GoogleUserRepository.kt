package com.sama.integration.google

import com.sama.users.domain.UserPublicDetails
import org.springframework.stereotype.Component

@Component
class GoogleUserRepository(private val googleServiceFactory: GoogleServiceFactory) {

    fun findUsingToken(accessToken: String): UserPublicDetails {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { UserPublicDetails(null, null, it.email, it.name) }
    }
}