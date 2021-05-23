package com.sama.users.domain

import com.sama.common.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    @Query("select nextval('sama.user_id_seq')", nativeQuery = true)
    fun nextIdentity(): Long

    @Query("select u.id from UserEntity u")
    fun findAllIds(): Set<Long>

    fun findByEmail(email: String): UserEntity?

    fun existsByEmail(email: String): Boolean

    @Query("select u.id from UserEntity u where email = ?1")
    fun findIdByEmail(email: String): Long?
}

fun UserRepository.findByEmailOrThrow(email: String) = findByEmail(email)
    ?: throw NotFoundException(UserEntity::class, "email", email)