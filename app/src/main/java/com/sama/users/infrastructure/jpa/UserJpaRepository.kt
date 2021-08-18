package com.sama.users.infrastructure.jpa

import com.sama.common.NotFoundException
import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, UserId> {

    @Query("select u.id from UserEntity u")
    fun findAllIds(): Set<UserId>

    @Query("select new com.sama.users.domain.UserDetails(u.id, u.publicId, u.email, u.fullName) from UserEntity u where id in ?1")
    fun findPublicDetailsById(ids: Set<UserId>): List<UserDetails>

    fun findByEmail(email: String): UserEntity?

    fun findByPublicId(publicId: UserPublicId): UserEntity?

    fun existsByEmail(email: String): Boolean

    @Query("select u.id from UserEntity u where email = ?1")
    fun findIdByEmail(email: String): UserId?

    @Query("select u.id from UserEntity u where publicId = ?1")
    fun findIdByPublicId(userPublicId: UserPublicId): UserId?
}

fun UserJpaRepository.findByEmailOrThrow(email: String) = findByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)

fun UserJpaRepository.findIdByEmailOrThrow(email: String) = findIdByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)

fun UserJpaRepository.findByPublicIdOrThrow(publicId: UserPublicId) = findByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)

fun UserJpaRepository.findIdByPublicIdOrThrow(publicId: UserPublicId) = findIdByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)
