package com.sama.integration.google.calendar.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.common.findByIdOrThrow
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.calendar.domain.Channel
import com.sama.integration.google.calendar.domain.ChannelRepository
import com.sama.integration.google.calendar.domain.ResourceType
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(classes = [ChannelRepository::class])
class ChannelRepositoryIT : BasePersistenceIT<ChannelRepository>() {

    @Test
    fun save() {
        val channel = Channel.new(
            UUID.randomUUID(),
            GoogleAccountId(1L),
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
        val accountId = GoogleAccountId(1L)
        val channel = Channel.new(
            channelId,
            accountId,
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
        val accountId = GoogleAccountId(1L)
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
            GoogleAccountId(1L),
            "token",
            ResourceType.CALENDAR,
            "primary",
            "some resource",
            Instant.now()
        )
        underTest.save(channel)

        underTest.delete(channel)
    }
}