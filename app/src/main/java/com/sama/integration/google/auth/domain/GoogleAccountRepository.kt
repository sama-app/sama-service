package com.sama.integration.google.auth.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.data.repository.Repository

@DomainRepository
interface GoogleAccountRepository : Repository<GoogleAccount, GoogleAccountId> {
    fun findByIdOrThrow(googleAccountId: GoogleAccountId): GoogleAccount
    fun findByPublicIdOrThrow(googleAccountId: GoogleAccountPublicId): GoogleAccount
    fun findAllByUserId(userId: UserId): Collection<GoogleAccount>
    fun findAllIds(): Collection<GoogleAccountId>
    fun findByUserIdAndPrimary(userId: UserId): GoogleAccountId?
    fun save(googleAccount: GoogleAccount): GoogleAccount
}