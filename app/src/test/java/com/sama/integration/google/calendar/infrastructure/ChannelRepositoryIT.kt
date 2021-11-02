package com.sama.integration.google.calendar.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.findByIdOrThrow
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.auth.infrastructure.JdbcGoogleAccountRepository
import com.sama.integration.google.calendar.domain.Channel
import com.sama.integration.google.calendar.domain.ChannelRepository
import com.sama.integration.google.calendar.domain.ResourceType
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.toUserId
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(classes = [ChannelRepository::class, JdbcGoogleAccountRepository::class])
class ChannelRepositoryIT : BasePersistenceIT<ChannelRepository>() {

    @Autowired
    lateinit var userRepository: UserJpaRepository

    @Autowired
    lateinit var googleAccountRepository: GoogleAccountRepository

    private lateinit var user: UserEntity
    private lateinit var googleAccount: GoogleAccount

    @BeforeEach
    fun setup() {
        val email = "one@meetsama.com"
        user = userRepository.save(UserEntity(email))
        userRepository.flush()
        googleAccount = googleAccountRepository.save(GoogleAccount.new(user.id!!.toUserId(), email, true))
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        userRepository.flush()
    }

    @Test
    fun save() {
        val channel = Channel.new(
            UUID.randomUUID(),
            googleAccount.id!!,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        val persisted = underTest.save(channel)

        assertThat(persisted).isEqualTo(channel)
    }


    @Test
    fun find() {
        val channelId = UUID.randomUUID()
        val channel = Channel.new(
            channelId,
            googleAccount.id!!,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        underTest.save(channel)

        val persisted = underTest.findByIdOrThrow(channelId)
        assertThat(persisted.channelId).isEqualTo(channel.channelId)
    }

    @Test
    fun findByGoogleAccountIdAndResourceType() {
        val accountId = googleAccount.id!!
        val channelOneId = UUID.randomUUID()
        val channelOne = Channel.new(
            channelOneId,
            accountId,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        val channelTwoId = UUID.randomUUID()
        val channelTwo = Channel.new(
            channelTwoId,
            accountId,
            "token2",
            ResourceType.CALENDAR,
            "primary2",
            "some resource",
            Instant.now()
        )
        underTest.save(channelOne)
        underTest.save(channelTwo)

        var channels = underTest.findByGoogleAccountIdAndResourceType(accountId, ResourceType.CALENDAR)

        assertThat(channels.map { it.channelId }).containsExactlyInAnyOrder(channelOneId, channelTwoId)

        channels = underTest.findByGoogleAccountIdAndResourceType(accountId, ResourceType.CALENDAR_LIST)
        assertThat(channels).isEmpty()

        channels = underTest.findByGoogleAccountIdAndResourceType(GoogleAccountId(999L), ResourceType.CALENDAR)
        assertThat(channels).isEmpty()
    }

    @Test
    fun delete() {
        val channel = Channel.new(
            UUID.randomUUID(),
            googleAccount.id!!,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        underTest.save(channel)

        underTest.delete(channel)
    }

    @Test
    fun deleteClosed() {
        val channel = Channel.new(
            UUID.randomUUID(),
            googleAccount.id!!,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        val closedChannel = Channel.new(
            UUID.randomUUID(),
            googleAccount.id!!,
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )

        // insert
        underTest.save(channel)
        underTest.save(closedChannel)
        // update
        underTest.save(closedChannel.close())
        val actual = underTest.deleteAllClosed()
        assertThat(actual).isEqualTo(1)
        assertThat(underTest.findAll()).hasSize(1)
    }
}