package com.sama.comms.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.sama.comms.domain.Notification
import com.sama.comms.domain.NotificationSender
import com.sama.users.application.UserApplicationService
import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.RuntimeException

@Component
class FirebaseNotificationSender(private val userApplicationService: UserApplicationService) : NotificationSender {
    private var logger: Logger = LoggerFactory.getLogger(FirebaseNotificationSender::class.java)

    override fun send(userId: UserId, notification: Notification): Boolean {
        return kotlin.runCatching {
            val token = (userApplicationService.findUserDeviceRegistrations(userId).firebaseDeviceRegistration
                ?.registrationToken
                ?: throw RuntimeException("No Firebase device registration found"))

            val message: Message = Message.builder()
                .setNotification(
                    com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build()
                )
                .setToken(token)
                .build()

            FirebaseMessaging.getInstance().send(message)
        }
            .onSuccess { logger.debug("Notification sent to User#$userId") }
            .onFailure { logger.warn("Could not send Firebase notification to User#$userId", it) }
            .isSuccess
    }
}