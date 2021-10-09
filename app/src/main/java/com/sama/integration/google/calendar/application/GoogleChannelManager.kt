package com.sama.integration.google.calendar.application

import com.sama.integration.google.ChannelConfiguration
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.calendar.domain.Channel
import com.sama.integration.google.calendar.domain.ChannelRepository
import com.sama.integration.google.calendar.domain.ResourceType
import com.sama.integration.google.calendar.domain.createCalendarsChannel
import com.sama.integration.google.calendar.domain.createEventsChannel
import com.sama.integration.google.calendar.domain.stopChannel
import com.sama.integration.google.translatedGoogleException
import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GoogleChannelManager(
    private val googleServiceFactory: GoogleServiceFactory,
    private val channelRepository: ChannelRepository,
    private val channelConfiguration: ChannelConfiguration
) {
    private var logger: Logger = LoggerFactory.getLogger(GoogleChannelManager::class.java)

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

        logger.info("Creating a Channel for GoogleAccount${accountId.id} $resourceType: $resourceId...")
        val calendarService = googleServiceFactory.calendarService(accountId)

        val googleChannel = try {
            when (resourceType) {
                ResourceType.CALENDAR_LIST -> {
                    calendarService.createCalendarsChannel(channelId, channelConfiguration.callbackUrl, token)
                }
                ResourceType.CALENDAR -> {
                    require(resourceId != null) { "Must provide a calendarId to open a Channel" }
                    calendarService.createEventsChannel(resourceId, channelId, channelConfiguration.callbackUrl, token)
                }
            }.execute()
        } catch (e: Exception) {
            logger.error("Error creating channel for GoogleAccount${accountId.id} $resourceType: $resourceId...", e)
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
}