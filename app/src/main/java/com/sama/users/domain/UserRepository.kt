package com.sama.users.domain

import com.sama.common.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, UserId> {
    @Query("select nextval('sama.user_id_seq')", nativeQuery = true)
    fun nextIdentity(): UserId

    @Query("select u.id from UserEntity u")
    fun findAllIds(): Set<UserId>

    @Query("select new com.sama.users.domain.BasicUserDetails(u.id, u.email, u.fullName) from UserEntity u where id in ?1")
    fun findBasicDetailsById(ids: Set<UserId>): List<BasicUserDetails>

    fun findByEmail(email: String): UserEntity?

    fun existsByEmail(email: String): Boolean

    @Query("select u.id from UserEntity u where email = ?1")
    fun findIdByEmail(email: String): UserId?

    @Query("select u.id from UserEntity u where email in ?1")
    fun findIdsByEmail(emails: Set<String>): Set<UserId>

    @Query("select new com.sama.users.domain.BasicUserDetails(u.id, u.email, u.fullName) from UserEntity u where email in ?1")
    fun findBasicDetailsByEmail(emails: Set<String>): List<BasicUserDetails>
}

fun UserRepository.findByEmailOrThrow(email: String) = findByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)


fun UserRepository.findIdByEmailOrThrow(email: String) = findIdByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)


data class UserEmail(val userId: UserId, val email: String)