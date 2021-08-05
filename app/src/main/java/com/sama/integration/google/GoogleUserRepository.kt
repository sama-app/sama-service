package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.oauth2.Oauth2
import com.sama.SamaApplication
import com.sama.users.domain.BasicUserDetails
import com.sama.users.domain.UserId
import org.springframework.stereotype.Component

@Component
class GoogleUserRepository(private val googleServiceFactory: GoogleServiceFactory) {

    fun findUsingToken(accessToken: String): BasicUserDetails {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { BasicUserDetails(null, it.email, it.name) }
    }

    fun find(userId: UserId): BasicUserDetails {
        val result = googleServiceFactory.oauth2Service(userId).userinfo().get().execute()
        return result.let { BasicUserDetails(userId, it.email, it.name) }
    }
}