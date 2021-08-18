package com.sama.users.domain

import com.sama.common.DomainRepository
import com.sama.users.infrastructure.jpa.UserEntity
import org.springframework.data.repository.Repository

@DomainRepository
interface UserRepository : Repository<UserDetails, UserId> {
    fun findByIdOrThrow(userId: UserId): UserDetails
    fun findByIds(ids: Collection<UserId>): List<UserDetails>
    fun findByEmailOrThrow(email: String): UserDetails
    fun findByPublicIdOrThrow(publicId: UserPublicId): UserDetails

    fun existsByEmail(email: String): Boolean

    fun findAllIds(): Set<UserId>
    fun findIdByPublicIdOrThrow(userPublicId: UserPublicId): UserId
    fun findIdByEmailOrThrow(email: String): UserId

    fun findJwtIssuerByIdOrThrow(userId: UserId): UserJwtIssuer
    fun findJwtIssuerByEmailOrThrow(email: String): UserJwtIssuer
    fun findJwtIssuerByPublicOrThrow(publicId: UserPublicId): UserJwtIssuer

    fun findDeviceRegistrationsByIdOrThrow(userId: UserId): UserDeviceRegistrations

    fun save(userDetails: UserDetails): UserDetails
    fun save(userGoogleCredential: UserGoogleCredential): UserGoogleCredential
    fun save(userDeviceRegistrations: UserDeviceRegistrations): UserDeviceRegistrations

    fun deleteAll()
}
