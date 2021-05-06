package com.sama.auth.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AuthUserRepository : JpaRepository<AuthUser, Long> {
    @Query("select au.id from AuthUser au")
    fun findAllIds(): Set<Long>

    fun findByEmail(email: String): AuthUser?

    @Query("select au.id from AuthUser au where email = ?1")
    fun findIdByEmail(email: String): Long?
}