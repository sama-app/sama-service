package com.sama.integration.google.calendar.application

import com.sama.common.NotFoundException
import com.sama.common.afterCommit
import com.sama.common.findByIdOrThrow
import com.sama.integration.google.calendar.domain.ChannelClosedException
import com.sama.integration.google.calendar.domain.ChannelRepository
import com.sama.integration.google.calendar.domain.ResourceType
import java.time.Instant
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


data class GoogleChannelNotification(
    val channelId: String,
    val token: String,
    val resourceId: String,
    val resourceState: String,
    val messageNumber: Long
)

@Component
class GoogleChannelNotificationReceiver(
    private val channelRepository: ChannelRepository,
    private val calendarSyncer: GoogleCalendarSyncer,
    private val channelManager: GoogleChannelManager,
    private val taskScheduler: TaskScheduler,
) {
    private var logger: Logger = LoggerFactory.getLogger(GoogleChannelNotificationReceiver::class.java)

    /**
     * https://developers.google.com/calendar/api/guides/push#receiving-notifications
     */
    @Transactional
    fun receive(notification: GoogleChannelNotification) {
        try {
            logger.info("Received notification for Channel#${notification.channelId}")
            val channelId = UUID.fromString(notification.channelId)
            val channel = try {
                channelRepository.findByIdOrThrow(channelId)
            } catch (e: NotFoundException) {
                logger.warn("Channel#${notification.channelId} received a message but it doesn't exist anymore")
                return
            }

            val updated = try {
                channel.receiveMessage(notification)
            } catch (e: ChannelClosedException) {
                logger.warn("Channel#${notification.channelId} received a message when mark as closed. Cleaning up...")
                channelManager.closeChannel(channel.googleAccountId, channel.resourceType, channel.resourceId)
                return
            }

            when (channel.resourceType) {
                ResourceType.CALENDAR_LIST -> {
                    calendarSyncer.markCalendarListNeedsSync(channel.googleAccountId)
                    afterCommit {
                        taskScheduler.schedule(
                            { calendarSyncer.syncUserCalendarList(channel.googleAccountId) },
                            Instant.now()
                        )
                    }
                }
                ResourceType.CALENDAR -> {
                    check(channel.resourceId != null) { "Received notification for invalid channel: ${channel.debugString()}" }
                    calendarSyncer.markCalendarNeedsSync(channel.googleAccountId, channel.resourceId)
                    afterCommit {
                        taskScheduler.schedule(
                            { calendarSyncer.syncUserCalendar(channel.googleAccountId, channel.resourceId) },
                            Instant.now()
                        )
                    }
                }
            }

            channelRepository.save(updated)
            logger.info("Finished processing notification for Channel#${notification.channelId}")
        } catch (e: Exception) {
            logger.error("Failed to process notification for Channel#${notification.channelId}", e)
            throw e
        }
    }
}