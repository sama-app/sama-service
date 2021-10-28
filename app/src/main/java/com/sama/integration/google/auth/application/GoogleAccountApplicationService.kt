package com.sama.integration.google.auth.application

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStoreFactory
import com.sama.common.ApplicationService
import com.sama.common.checkAccess
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.auth.domain.GoogleCredentialRepository
import com.sama.integration.google.auth.domain.toStorageKey
import com.sama.integration.google.calendar.application.GoogleCalendarSyncer
import com.sama.users.application.AuthUserService
import io.sentry.spring.tracing.SentryTransaction
import org.apache.commons.logging.LogFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
@ApplicationService
class GoogleAccountApplicationService(
    private val googleAccountRepository: GoogleAccountRepository,
    private val authUserService: AuthUserService,
    private val credentialDataStoreFactory: DataStoreFactory,
    private val googleServiceFactory: GoogleServiceFactory,
    private val googleCalendarSyncer: GoogleCalendarSyncer,
    transactionManager: PlatformTransactionManager,
) : GoogleAccountService {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val logger = LogFactory.getLog(javaClass)

    override fun getUserInfo(accessToken: String): GoogleUserInfo {
        val result = googleServiceFactory.oauth2Service(accessToken).userinfo().get().execute()
        return result.let { GoogleUserInfo(it.email, it.name) }
    }

    @Transactional(readOnly = true)
    override fun findAllLinked(): GoogleIntegrationsDTO {
        val userId = authUserService.currentUserId()
        return googleAccountRepository.findAllByUserId(userId)
            .filter { it.linked }
            .map { GoogleAccountDTO(it.publicId!!, it.email) }
            .let { GoogleIntegrationsDTO(it) }
    }

    @Transactional
    override fun linkAccount(command: LinkGoogleAccountCommand): GoogleAccountPublicId {
        val userId = authUserService.currentUserId()
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
    override fun unlinkAccount(command: UnlinkGoogleAccountCommand): Boolean {
        val userId = authUserService.currentUserId()
        val googleAccount = googleAccountRepository.findByPublicIdOrThrow(command.googleAccountId)
        checkAccess(googleAccount.userId == userId)
        return unlinkAccount(googleAccount)
    }

    @Transactional
    private fun unlinkAccount(googleAccount: GoogleAccount): Boolean {
        val updated = googleAccount.unlink()

        googleAccountRepository.save(updated)
        googleCalendarSyncer.disableCalendarListSync(updated.id!!)
        return true
    }

    @SentryTransaction(operation = "runAccountMaintenance")
    @Scheduled(cron = "0 0 */1 * * *")
    fun runAccountMaintenance() {
        val credentialRepository = credentialDataStoreFactory.getDataStore<StoredCredential>("credential")
                as GoogleCredentialRepository
        val accountIds = credentialRepository.findInvalidatedIds()
            .map { GoogleAccountId(it) }

        logger.debug("Unlinking ${accountIds.size} Google Accounts...")
        accountIds.forEach { googleAccountId ->
            try {
                logger.debug("Unlinking invalidated GoogleAccount#$googleAccountId")
                transactionTemplate.execute {
                    val googleAccount = googleAccountRepository.findByIdOrThrow(googleAccountId)
                    unlinkAccount(googleAccount)
                }
                logger.info("Unlinked invalidated GoogleAccount#$googleAccountId")
            } catch (e: Exception) {
                logger.error("Failed to unlink GoogleAccount#$googleAccountId", e)
            }
        }
        logger.info("Completed unlinking invalidated GoogleAccounts...")
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