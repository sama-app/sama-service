package com.sama.integration.google.user

import com.sama.integration.google.GoogleServiceFactory
import com.sama.users.domain.UserDetails
import org.springframework.stereotype.Component

@Component
class GoogleUserRepository(private val googleServiceFactory: GoogleServiceFactory) {

    fun findUsingToken(accessToken: String): UserDetails {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { UserDetails(null, null, it.email, it.name, true) }
    }
}