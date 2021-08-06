package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.connection.domain.UserConnection
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JdbcUserConnectionRepository::class])
class JdbcUserConnectionRepositoryTest : BasePersistenceIT<JdbcUserConnectionRepository>() {

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
    fun `save and find connected users`() {
        val userConnection1 = UserConnection(userOne.id!!, userTwo.id!!)
        val userConnection2 = UserConnection(userThree.id!!, userOne.id!!)

        underTest.save(userConnection1)
        underTest.save(userConnection2)

        val connectedUserIds = underTest.findConnectedUserIds(userOne.id!!)

        assertThat(connectedUserIds).containsExactly(userTwo.id!!, userThree.id!!)
    }

    @Test
    fun `delete user connection`() {

        val userConnection1 = UserConnection(userOne.id!!, userTwo.id!!)
        val userConnection2 = UserConnection(userOne.id!!, userThree.id!!)

        underTest.save(userConnection1)
        underTest.save(userConnection2)
        underTest.delete(userConnection1)

        val connectedUserIds = underTest.findConnectedUserIds(userOne.id!!)
        assertThat(connectedUserIds).containsExactly(userThree.id!!)
    }
}