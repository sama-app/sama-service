package com.sama.comms.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.sama.comms.domain.Notification
import com.sama.comms.domain.NotificationSender
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FirebaseNotificationSender(private val userService: UserService) : NotificationSender {
    private var logger: Logger = LoggerFactory.getLogger(FirebaseNotificationSender::class.java)

    override fun send(userId: UserId, notification: Notification) {
        val response = userService.findUserDeviceRegistrations(userId)
        if (response.firebaseDeviceRegistrations.isEmpty()) {
            throw RuntimeException("No Firebase device registrations found")
        }

        response.firebaseDeviceRegistrations.forEach {
            val message = Message.builder()
                .setNotification(
                    com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build()
                )
                .putAllData(notification.additionalData)
                .setToken(it.registrationToken)
                .build()
            try {
                FirebaseMessaging.getInstance().send(message)
                logger.debug("Notifications sent to User#${userId.id}  Device#${it.deviceId}")
            } catch (e: Exception) {
                if (e is FirebaseMessagingException) {
                    // Remove unregistered devices
                    if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                        kotlin.runCatching {
                            userService.unregisterDevice(userId, UnregisterDeviceCommand(it.deviceId))
                        }
                    }
                }
                val code = if (e is FirebaseMessagingException) e.messagingErrorCode.toString() else "OTHER"
                logger.warn("Could not send Firebase notification to User#${userId.id} Device#${it.deviceId}: $code", e)
            }
        }
    }
}