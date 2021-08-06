package com.sama.users.domain

import com.sama.common.NotFoundException
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, UserId> {
    @Query("select nextval('sama.user_id_seq')", nativeQuery = true)
    fun nextIdentity(): UserId

    @Query("select u.id from UserEntity u")
    fun findAllIds(): Set<UserId>

    @Query("select new com.sama.users.domain.UserPublicDetails(u.id, u.publicId, u.email, u.fullName) from UserEntity u where id in ?1")
    fun findPublicDetailsById(ids: Set<UserId>): List<UserPublicDetails>

    fun findByEmail(email: String): UserEntity?

    fun findByPublicId(publicId: UserPublicId): UserEntity?

    fun existsByEmail(email: String): Boolean

    @Query("select u.id from UserEntity u where email = ?1")
    fun findIdByEmail(email: String): UserId?

    @Query("select u.id from UserEntity u where publicId = ?1")
    fun findIdByPublicId(userPublicId: UserPublicId): UserId?
}

fun UserRepository.findByEmailOrThrow(email: String) = findByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)

fun UserRepository.findByPublicIdOrThrow(publicId: UserPublicId) = findByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)

fun UserRepository.findIdByPublicIdOrThrow(publicId: UserPublicId) = findIdByPublicId(publicId)
    ?: throw NotFoundException(UserEntity::class, "userId", publicId)

fun UserRepository.nextPublicId(): UserPublicId {
    return UUID.randomUUID()
}
