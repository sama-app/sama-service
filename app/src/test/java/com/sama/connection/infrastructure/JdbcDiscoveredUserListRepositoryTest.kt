package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.connection.domain.DiscoveredUserList
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JdbcDiscoveredUserListRepository::class])
class JdbcDiscoveredUserListRepositoryTest : BasePersistenceIT<JdbcDiscoveredUserListRepository>() {

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setup() {
        userRepository.save(UserEntity(1L, "one@meetsama.com").apply { fullName = "One" })
        userRepository.save(UserEntity(2L, "two@meetsama.com").apply { fullName = "Two" })
        userRepository.save(UserEntity(3L, "three@meetsama.com").apply { fullName = "Three" })
        userRepository.flush()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `save user connections`() {
        val expected = DiscoveredUserList(1L, setOf(2L, 3L))
        underTest.save(expected)

        val actual = underTest.findById(1L)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `save no user connections`() {
        val expected = DiscoveredUserList(1L, emptySet())
        underTest.save(expected)

        val actual = underTest.findById(1L)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `remove user connections`() {
        val initial = DiscoveredUserList(1L, setOf(2L, 3L))
        underTest.save(initial)

        val expected = initial.copy(discoveredUsers = setOf(2L))
        underTest.save(expected)

        val actual = underTest.findById(1L)
        assertThat(actual).isEqualTo(expected)
    }
}