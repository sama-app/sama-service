package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.connection.domain.UserConnection
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.toUserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [JdbcUserConnectionRepository::class])
class JdbcUserConnectionRepositoryIT : BasePersistenceIT<JdbcUserConnectionRepository>() {

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
    fun `save and find connected user ids`() {
        val userConnection1 = UserConnection(userOne.id!!.toUserId(), userTwo.id!!.toUserId())
        val userConnection2 = UserConnection(userThree.id!!.toUserId(), userOne.id!!.toUserId())

        underTest.save(userConnection1)
        underTest.save(userConnection2)

        assertThat(underTest.findConnectedUserIds(userOne.id!!.toUserId()))
            .containsExactlyInAnyOrder(userTwo.id!!.toUserId(), userThree.id!!.toUserId())

        assertThat(underTest.findConnectedUserIds(userTwo.id!!.toUserId()))
            .containsExactlyInAnyOrder(userOne.id!!.toUserId())

        assertThat(underTest.findConnectedUserIds(userThree.id!!.toUserId()))
            .containsExactlyInAnyOrder(userOne.id!!.toUserId())
    }

    @Test
    fun delete() {

        val userConnection1 = UserConnection(userOne.id!!.toUserId(), userTwo.id!!.toUserId())
        val userConnection2 = UserConnection(userOne.id!!.toUserId(), userThree.id!!.toUserId())

        underTest.save(userConnection1)
        underTest.save(userConnection2)
        underTest.delete(userConnection1)

        val connectedUserIds = underTest.findConnectedUserIds(userOne.id!!.toUserId())
        assertThat(connectedUserIds).containsExactly(userThree.id!!.toUserId())
    }
}