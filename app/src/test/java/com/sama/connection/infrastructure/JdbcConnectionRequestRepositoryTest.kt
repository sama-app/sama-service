package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.NotFoundException
import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestStatus.APPROVED
import com.sama.connection.domain.ConnectionRequestStatus.PENDING
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(classes = [JdbcConnectionRequestRepository::class])
class JdbcConnectionRequestRepositoryTest : BasePersistenceIT<JdbcConnectionRequestRepository>() {
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
    fun `save and find by id`() {
        val id = UUID.randomUUID()
        val expected = ConnectionRequest(id, 1L, 2L, PENDING)
        underTest.save(expected)

        val actual = underTest.findByIdOrThrow(id)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `find by non-existent id`() {
        assertThrows<NotFoundException> {
            underTest.findByIdOrThrow(UUID.randomUUID())
        }
    }

    @Test
    fun `find pending by user ids`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, 1L, 2L, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByUserIds(1L, 2L)
        assertThat(actual).isEqualTo(connectionRequest)
    }

    @Test
    fun `find pending by initiator id`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, 1L, 2L, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByInitiatorId(1L)
        assertThat(actual).containsExactly(connectionRequest)
    }


    @Test
    fun `find pending by recipient id`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, 1L, 2L, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByRecipientId(2L)
        assertThat(actual).containsExactly(connectionRequest)
    }

    @Test
    fun `does not exists pending by user ids`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, 1L, 2L, APPROVED)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByUserIds(1L, 2L)
        assertThat(actual).isNull()
    }

    @Test
    fun `update status`() {
        val id = UUID.randomUUID()
        val initial = ConnectionRequest(id, 1L, 2L, PENDING)
        underTest.save(initial)

        val expected = initial.copy(status = APPROVED)
        underTest.save(expected)

        val actual = underTest.findByIdOrThrow(id)

        assertThat(actual).isEqualTo(expected)
    }
}