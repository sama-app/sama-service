package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.connection.domain.DiscoveredUserList
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.toUserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JdbcDiscoveredUserListRepository::class])
class JdbcDiscoveredUserListRepositoryIT : BasePersistenceIT<JdbcDiscoveredUserListRepository>() {

    @Autowired
    lateinit var userRepository: UserJpaRepository

    private lateinit var userOne: UserEntity
    private lateinit var userTwo: UserEntity
    private lateinit var userThree: UserEntity

    @BeforeEach
    fun setup() {
        userOne = userRepository.save(UserEntity("one@meetsama.com"))
        userTwo = userRepository.save(UserEntity("two@meetsama.com"))
        userThree = userRepository.save(UserEntity("three@meetsama.com"))
        userRepository.flush()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `save and find`() {
        val expected = DiscoveredUserList(userOne.id!!.toUserId(), setOf(userTwo.id!!.toUserId(), userThree.id!!.toUserId()))
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!.toUserId())

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `save empty list`() {
        val expected = DiscoveredUserList(userOne.id!!.toUserId(), emptySet())
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!.toUserId())

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `remove from list and find`() {
        val initial = DiscoveredUserList(userOne.id!!.toUserId(), setOf(userTwo.id!!.toUserId(), userThree.id!!.toUserId()))
        underTest.save(initial)

        val expected = initial.copy(discoveredUsers = setOf(userTwo.id!!.toUserId()))
        underTest.save(expected)

        val actual = underTest.findById(userOne.id!!.toUserId())
        assertThat(actual).isEqualTo(expected)
    }
}