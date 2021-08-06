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
}