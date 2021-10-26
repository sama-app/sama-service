package com.sama.integration.google.auth.application

import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.users.domain.UserId
import org.springframework.transaction.annotation.Transactional

interface GoogleAccountService {
    fun getUserInfo(accessToken: String): GoogleUserInfo
    fun findAllLinked(userId: UserId): GoogleIntegrationsDTO
    fun linkAccount(userId: UserId, command: LinkGoogleAccountCommand): GoogleAccountPublicId
    fun unlinkAccount(userId: UserId, command: UnlinkGoogleAccountCommand): Boolean
}