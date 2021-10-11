package com.sama.integration.google.calendar.application

import com.sama.integration.google.ChannelConfiguration
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.domain.Channel
import com.sama.integration.google.calendar.domain.ChannelRepository
import com.sama.integration.google.calendar.domain.ResourceType
import com.sama.integration.google.calendar.domain.createCalendarsChannel
import com.sama.integration.google.calendar.domain.createEventsChannel
import com.sama.integration.google.calendar.domain.stopChannel
import com.sama.integration.google.translatedGoogleException
import com.sama.integration.sentry.sentrySpan
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class GoogleChannelManager(
    private val googleServiceFactory: GoogleServiceFactory,
    private val channelRepository: ChannelRepository,
    private val channelConfiguration: ChannelConfiguration,
    private val googleAccountRepository: GoogleAccountRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val logger: Logger = LoggerFactory.getLogger(GoogleChannelManager::class.java)

    fun createChannel(accountId: GoogleAccountId, resourceType: ResourceType, resourceId: String? = null) {
        if (!channelConfiguration.enabled) {
            return
        }

        try {
            closeChannel(accountId, resourceType, resourceId)
        } catch (e: Exception) {
            logger.debug("Could not close channel for GoogleAccount${accountId.id} $resourceType: $resourceId: ${e.message}", e)
        }

        val channelId = Channel.newId()
        val token = Channel.newToken()
        val expiredAt = Instant.now().plus(channelConfiguration.expiresInHours, HOURS)

        logger.info("Creating a Channel for GoogleAccount${accountId.id} $resourceType: $resourceId...")
        val calendarService = googleServiceFactory.calendarService(accountId)

        val googleChannel = try {
            when (resourceType) {
                ResourceType.CALENDAR_LIST -> {
                    calendarService.createCalendarsChannel(channelId, token, channelConfiguration.callbackUrl, expiredAt)
                }
                ResourceType.CALENDAR -> {
                    require(resourceId != null) { "Must provide a calendarId to open a Channel" }
                    calendarService.createEventsChannel(resourceId, channelId, token, channelConfiguration.callbackUrl, expiredAt)
                }
            }.execute()
        } catch (e: Exception) {
            logger.error("Error creating channel for GoogleAccount#${accountId.id} $resourceType: $resourceId...", e)
            throw translatedGoogleException(e)
        }

        try {
            // Sanity check Google returns the expected data
            check(channelId.toString() == googleChannel.id)
            check(token == googleChannel.token)

            val channel = Channel.new(
                channelId,
                accountId,
                token,
                resourceType,
                resourceId,
                googleChannel.resourceId,
                Instant.ofEpochMilli(googleChannel.expiration)
            )
            channelRepository.save(channel)
            logger.info("Channel created for ${channel.debugString()}...")
        } catch (e: Exception) {
            calendarService.stopChannel(channelId, googleChannel.resourceId)
            logger.warn("Reverted channel creation for GoogleAccount${accountId.id} $resourceType: $resourceId...", e)
            throw e
        }
    }

    fun closeChannel(accountId: GoogleAccountId, resourceType: ResourceType, resourceId: String? = null) {
        if (!channelConfiguration.enabled) {
            return
        }

        val channels = channelRepository.findByGoogleAccountIdAndResourceType(accountId, resourceType)

        channels.forEach { channel ->
            try {
                val calendarService = googleServiceFactory.calendarService(accountId)
                calendarService.stopChannel(channel.id, channel.externalResourceId)
                channelRepository.save(channel.close())
            } catch (e: Exception) {
                logger.error("Error closing channel for ${channel.debugString()}...", e)
                throw translatedGoogleException(e)
            }

            logger.info("Channel closed for ${channel.debugString()}...")
        }
    }

    private fun recreateChannel(channel: Channel) {
        val googleAccount = googleAccountRepository.findByIdOrThrow(channel.googleAccountId)
        if (!googleAccount.linked) {
            closeChannel(channel.googleAccountId, channel.resourceType, channel.resourceId)
            return
        }
        createChannel(channel.googleAccountId, channel.resourceType, channel.resourceId)
    }

    @Scheduled(cron = "0 0 */3 * * *")
    fun runChannelMaintenance() {
        sentrySpan(method = "runChannelMaintenance") {
            val channels = channelRepository.findByExpiresAtLessThan(Instant.now().plus(3, DAYS))
            channels.forEach { channel ->
                transactionTemplate.execute { recreateChannel(channel) }
            }
        }
    }
}