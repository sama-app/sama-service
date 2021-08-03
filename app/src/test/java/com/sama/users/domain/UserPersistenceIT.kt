package com.sama.users.domain

import com.sama.common.BasePersistenceIT
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserPersistenceIT : BasePersistenceIT<UserRepository>() {

    @Test
    fun `next identity`() {
        val one = underTest.nextIdentity()
        val two = underTest.nextIdentity()
        assertNotEquals(one, two)
    }


    @Test
    fun `find basic details by emails`() {
        underTest.save(UserEntity(1L, "one@meetsama.com").apply { fullName = "One" })
        underTest.save(UserEntity(2L, "two@meetsama.com").apply { fullName = "Two" })

        val result = underTest.findBasicDetailsByEmail(setOf("one@meetsama.com", "two@meetsama.com", "three@meetsama.com"))

        assertThat(result).containsExactly(
            BasicUserDetails(1L, "one@meetsama.com", "One"),
            BasicUserDetails(2L, "two@meetsama.com", "Two")
        )
    }
}