package com.sama.users.domain

import com.sama.common.BasePersistenceIT
import com.sama.users.infrastructure.UserRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [UserRepositoryImpl::class])
class UserPersistenceIT : BasePersistenceIT<UserRepository>() {

    @Test
    fun save() {
        val email = "test@meetsama.com"
        val fullName = "Test"
        val persisted = underTest.save(UserDetails(email = email, fullName = fullName, active = true))

        assertThat(persisted.id).isNotNull
        assertThat(persisted.publicId).isNotNull
        assertThat(persisted.email).isEqualTo(email)
        assertThat(persisted.fullName).isEqualTo(fullName)
        assertThat(persisted.active).isTrue
    }
}