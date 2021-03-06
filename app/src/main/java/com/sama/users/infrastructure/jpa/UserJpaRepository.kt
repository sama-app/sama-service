package com.sama.users.infrastructure.jpa

import com.sama.common.NotFoundException
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    @Query("select u.id from UserEntity u")
    fun findAllIds(): Set<Long>

    fun findByEmail(email: String): UserEntity?

    fun findByPublicId(publicId: UUID): UserEntity?

    fun existsByEmail(email: String): Boolean

    @Query("select u.id from UserEntity u where email = ?1")
    fun findIdByEmail(email: String): Long?

    @Query("select u.id from UserEntity u where email IN (?1)")
    fun findIdsByEmail(emails: Set<String>): Set<Long>

    @Query("select u.id from UserEntity u where publicId = ?1")
    fun findIdByPublicId(userPublicId: UUID): Long?

    @Modifying(clearAutomatically = true)
    @Query(
        "DELETE FROM sama.user_firebase_credential WHERE user_id != :ownerUserId AND device_id IN :deviceIds",
        nativeQuery = true
    )
    fun deleteFirebaseCredentials(ownerUserId: Long, deviceIds: Collection<UUID>)
}

fun UserJpaRepository.findByEmailOrThrow(email: String) = findByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)

fun UserJpaRepository.findIdByEmailOrThrow(email: String) = findIdByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)

fun UserJpaRepository.findByPublicIdOrThrow(publicId: UUID) = findByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)

fun UserJpaRepository.findIdByPublicIdOrThrow(publicId: UUID) = findIdByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)
