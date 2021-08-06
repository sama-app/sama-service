package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.connection.domain.DiscoveredUserList
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import java.util.UUID
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

    private lateinit var userOne: UserEntity
    private lateinit var userTwo: UserEntity
    private lateinit var userThree: UserEntity

    @BeforeEach
    fun setup() {
        userOne = userRepository.save(UserEntity("one@meetsama.com").apply { fullName = "One" })
        userTwo = userRepository.save(UserEntity("two@meetsama.com").apply { fullName = "Two" })
        userThree = userRepository.save(UserEntity("three@meetsama.com").apply { fullName = "Three" })
        userRepository.flush()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `save user connections`() {
        val expected = DiscoveredUserList(userOne.id!!, setOf(userTwo.id!!, userThree.id!!))
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `save no user connections`() {
        val expected = DiscoveredUserList(userOne.id!!, emptySet())
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `remove user connections`() {
        val initial = DiscoveredUserList(userOne.id!!, setOf(userTwo.id!!, userThree.id!!))
        underTest.save(initial)

        val expected = initial.copy(discoveredUsers = setOf(userTwo.id!!))
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!)
        assertThat(actual).isEqualTo(expected)
    }
}