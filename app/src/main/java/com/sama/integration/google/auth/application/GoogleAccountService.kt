package com.sama.integration.google.auth.application

import com.sama.common.InternalApi
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.users.domain.UserId

interface GoogleAccountService {
    fun getUserInfo(accessToken: String): GoogleUserInfo
    fun findAllLinked(): GoogleIntegrationsDTO

    @InternalApi
    fun linkAccount(userId: UserId, command: LinkGoogleAccountCommand): GoogleAccountPublicId
    fun unlinkAccount(command: UnlinkGoogleAccountCommand): Boolean
}