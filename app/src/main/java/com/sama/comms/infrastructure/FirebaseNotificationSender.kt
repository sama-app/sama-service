package com.sama.comms.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.sama.comms.domain.Notification
import com.sama.comms.domain.NotificationSender
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FirebaseNotificationSender(private val userService: UserService) : NotificationSender {
    private var logger: Logger = LoggerFactory.getLogger(FirebaseNotificationSender::class.java)

    override fun send(receiverUserId: UserId, notification: Notification): Boolean {
        return kotlin.runCatching {
            val token = userService.findUserDeviceRegistrations(receiverUserId).firebaseDeviceRegistration
                ?.registrationToken
                ?: throw RuntimeException("No Firebase device registration found")

            val message: Message = Message.builder()
                .setNotification(
                    com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build()
                )
                .putAllData(notification.additionalData)
                .setToken(token)
                .build()

            FirebaseMessaging.getInstance().send(message)
        }
            .onSuccess { logger.debug("Notification sent to User#${receiverUserId.id}") }
            .onFailure { logger.warn("Could not send Firebase notification to User#${receiverUserId.id}", it) }
            .isSuccess
    }
}