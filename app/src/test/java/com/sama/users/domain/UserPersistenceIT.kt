package com.sama.users.domain

import com.sama.common.BasePersistenceIT
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserPersistenceIT : BasePersistenceIT<UserRepository>() {

    @Test
    fun save() {
        val email = "test@meetsama.com"
        val fullName = "Test"
        val userEntity = underTest.save(UserEntity(email).also { it.fullName = fullName })

        assertThat(userEntity.id).isNotNull()
        assertThat(userEntity.publicId).isNotNull()
        assertThat(userEntity.email).isEqualTo(email)
        assertThat(userEntity.fullName).isEqualTo(fullName)
    }
}