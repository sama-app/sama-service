package com.sama.integration.google.user

import com.sama.integration.google.GoogleServiceFactory
import org.springframework.stereotype.Component

@Component
class GoogleUserService(private val googleServiceFactory: GoogleServiceFactory) {

    fun findUsingToken(accessToken: String): GoogleUser {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { GoogleUser(it.email, it.name) }
    }
}

data class GoogleUser(val email: String, val fullName: String?)