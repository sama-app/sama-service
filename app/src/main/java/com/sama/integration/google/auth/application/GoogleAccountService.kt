package com.sama.integration.google.auth.application

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStoreFactory
import com.sama.common.ApplicationService
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.auth.domain.toStorageKey
import com.sama.integration.google.calendar.application.GoogleCalendarSyncer
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ApplicationService
class GoogleAccountService(
    private val googleAccountRepository: GoogleAccountRepository,
    private val credentialDataStoreFactory: DataStoreFactory,
    private val googleServiceFactory: GoogleServiceFactory,
    private val googleCalendarSyncer: GoogleCalendarSyncer
) {
    fun getUserInfo(accessToken: String): GoogleUserInfo {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { GoogleUserInfo(it.email, it.name) }
    }

    @Transactional(readOnly = true)
    fun findAll(userId: UserId): Collection<GoogleAccountDTO> {
        return googleAccountRepository.findAllByUserId(userId)
            .filter { it.linked }
            .map { GoogleAccountDTO(it.publicId!!, it.email) }
    }

    @Transactional
    fun linkAccount(userId: UserId, command: LinkGoogleAccountCommand): GoogleAccountPublicId {
        val googleAccounts = googleAccountRepository.findAllByUserId(userId)
        val existingGoogleAccount = googleAccounts.find { it.email == command.email }

        val isFirstAccount = googleAccounts.isEmpty()
        val googleAccount = existingGoogleAccount?.link()
            ?: GoogleAccount.new(userId, command.email, isFirstAccount)

        val updated = googleAccountRepository.save(googleAccount)
        val googleAccountId = updated.id!!

        persistGoogleCredential(googleAccountId, command.credential)
        googleCalendarSyncer.enableCalendarListSync(googleAccountId)

        return updated.publicId!!
    }

    @Transactional
    fun unlinkAccount(userId: UserId, command: UnlinkGoogleAccountCommand): Boolean {
        val googleAccount = googleAccountRepository.findByPublicIdOrThrow(command.googleAccountId)
        val updated = googleAccount.unlink()

        googleAccountRepository.save(updated)
        googleCalendarSyncer.disableCalendarListSync(updated.id!!)
        deleteGoogleCredential(updated.id)
        return true
    }

    private fun persistGoogleCredential(googleAccountId: GoogleAccountId, credential: GoogleOauth2Credential) {
        val credentialStore = credentialDataStoreFactory.getDataStore<StoredCredential>("credential")
        credentialStore.set(
            googleAccountId.toStorageKey(),
            StoredCredential().apply {
                accessToken = credential.accessToken
                refreshToken = credential.refreshToken
                expirationTimeMilliseconds = credential.expirationTimeMs
            })
    }

    private fun deleteGoogleCredential(googleAccountId: GoogleAccountId) {
        credentialDataStoreFactory.getDataStore<StoredCredential>("credential")
            .delete(googleAccountId.toStorageKey())
    }
}