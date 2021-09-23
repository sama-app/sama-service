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

    fun findIdByPublicIdOrThrow(userPublicId: UserPublicId): UserId
    fun findIdByEmailOrThrow(email: String): UserId

    fun findDeviceRegistrationsByIdOrThrow(userId: UserId): UserDeviceRegistrations

    fun save(userDetails: UserDetails): UserDetails
    fun save(user: UserDeviceRegistrations): UserDeviceRegistrations

    fun deleteAll()
}
