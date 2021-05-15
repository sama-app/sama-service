package com.sama.users.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query("select au.id from User au")
    fun findAllIds(): Set<Long>

    fun findByEmail(email: String): User?

    @Query("select au.id from User au where email = ?1")
    fun findIdByEmail(email: String): Long?
}