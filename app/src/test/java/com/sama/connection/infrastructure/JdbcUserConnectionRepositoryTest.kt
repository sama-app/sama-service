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

    @BeforeEach
    fun setup() {
        userRepository.save(UserEntity(1L, UUID.randomUUID(), "one@meetsama.com").apply { fullName = "One" })
        userRepository.save(UserEntity(2L, UUID.randomUUID(), "two@meetsama.com").apply { fullName = "Two" })
        userRepository.save(UserEntity(3L, UUID.randomUUID(), "three@meetsama.com").apply { fullName = "Three" })
        userRepository.flush()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun `save and find connected users`() {
        val userConnection1 = UserConnection(1L, 2L)
        val userConnection2 = UserConnection(3L, 1L)

        underTest.save(userConnection1)
        underTest.save(userConnection2)

        val connectedUserIds = underTest.findConnectedUserIds(1L)

        assertThat(connectedUserIds).containsExactly(2L, 3L)
    }

    @Test
    fun `delete user connection`() {

        val userConnection1 = UserConnection(1L, 2L)
        val userConnection2 = UserConnection(1L, 3L)

        underTest.save(userConnection1)
        underTest.save(userConnection2)
        underTest.delete(userConnection1)

        val connectedUserIds = underTest.findConnectedUserIds(1L)
        assertThat(connectedUserIds).containsExactly(3L)
    }
}