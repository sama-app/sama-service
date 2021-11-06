package com.sama.comms.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.sama.comms.domain.NotificationSender
import com.sama.users.application.UnregisterDeviceCommand
import com.sama.users.application.UserDeviceRegistrationService
import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

typealias FirebaseMessage = com.google.firebase.messaging.Message
typealias FirebaseNotification = com.google.firebase.messaging.Notification

@Component
class FirebaseNotificationSender(private val deviceRegistrationService: UserDeviceRegistrationService) : NotificationSender {
    private var logger: Logger = LoggerFactory.getLogger(FirebaseNotificationSender::class.java)

    override fun send(receiverUserId: UserId, message: com.sama.comms.domain.Message) {
        val response = deviceRegistrationService.find(receiverUserId)
        if (response.firebaseDeviceRegistrations.isEmpty()) {
            logger.info("No Firebase device registrations found for User#${receiverUserId.id}")
            return
        }

        response.firebaseDeviceRegistrations.forEach { (deviceId, registrationToken) ->
            val messageToSend = FirebaseMessage.builder().apply {
                setToken(registrationToken)
                putAllData(message.additionalData)

                message.notification?.let {
                    setNotification(
                        FirebaseNotification.builder()
                            .setTitle(it.title)
                            .setBody(it.body)
                            .build()
                    )
                }
            }.build()

            try {
                FirebaseMessaging.getInstance().send(messageToSend)
                logger.debug("Notifications sent to User#${receiverUserId.id}  Device#${deviceId}")
            } catch (e: Exception) {
                if (e is FirebaseMessagingException) {
                    // Remove unregistered devices
                    if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                        kotlin.runCatching {
                            deviceRegistrationService.unregister(UnregisterDeviceCommand(deviceId))
                        }.onFailure {
                            logger.error("Error unregistering device#${deviceId}", it)
                        }
                    }
                }
                val code = if (e is FirebaseMessagingException) e.messagingErrorCode.toString() else "OTHER"
                logger.warn("Could not send Firebase notification to User#${receiverUserId.id} Device#${deviceId}: $code", e)
            }
        }
    }
}