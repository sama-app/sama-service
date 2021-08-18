package com.sama.connection.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.NotFoundException
import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestStatus.APPROVED
import com.sama.connection.domain.ConnectionRequestStatus.PENDING
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
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
    lateinit var userRepository: UserJpaRepository

    private lateinit var userOne: UserEntity
    private lateinit var userTwo: UserEntity

    @BeforeEach
    fun setup() {
        userOne = userRepository.save(UserEntity("one@meetsama.com").apply { fullName = "One" })
        userTwo = userRepository.save(UserEntity("two@meetsama.com").apply { fullName = "Two" })
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
        val expected = ConnectionRequest(id, userOne.id!!, userTwo.id!!, PENDING)
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
        val connectionRequest = ConnectionRequest(id, userOne.id!!, userTwo.id!!, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByUserIds(userOne.id!!, userTwo.id!!)
        assertThat(actual).isEqualTo(connectionRequest)
    }

    @Test
    fun `find pending by initiator id`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, userOne.id!!, userTwo.id!!, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByInitiatorId(userOne.id!!)
        assertThat(actual).containsExactly(connectionRequest)
    }


    @Test
    fun `find pending by recipient id`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, userOne.id!!, userTwo.id!!, PENDING)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByRecipientId(userTwo.id!!)
        assertThat(actual).containsExactly(connectionRequest)
    }

    @Test
    fun `does not exists pending by user ids`() {
        val id = UUID.randomUUID()
        val connectionRequest = ConnectionRequest(id, userOne.id!!, userTwo.id!!, APPROVED)
        underTest.save(connectionRequest)

        val actual = underTest.findPendingByUserIds(userOne.id!!, userTwo.id!!)
        assertThat(actual).isNull()
    }

    @Test
    fun update() {
        val id = UUID.randomUUID()
        val initial = ConnectionRequest(id, userOne.id!!, userTwo.id!!, PENDING)
        underTest.save(initial)

        val expected = initial.copy(status = APPROVED)
        underTest.save(expected)

        val actual = underTest.findByIdOrThrow(id)

        assertThat(actual).isEqualTo(expected)
    }
}