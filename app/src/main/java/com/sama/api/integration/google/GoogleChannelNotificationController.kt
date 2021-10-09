package com.sama.api.integration.google

import com.sama.integration.google.calendar.application.GoogleChannelNotification
import com.sama.integration.google.calendar.application.GoogleChannelNotificationReceiver
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@Hidden
@Tag(name = "integration.google")
@RestController
class GoogleChannelNotificationController(
    private val channelNotificationReceiver: GoogleChannelNotificationReceiver
) {

    @Operation(summary = "Receive push notification from Google Calendar API")
    @PostMapping("/api/integration/google/channel-notification")
    fun receiveChannelNotification(
        @RequestHeader("X-Goog-Channel-ID") channelId: String,
        @RequestHeader("X-Goog-Channel-Token") token: String,
        @RequestHeader("X-Goog-Resource-ID") resourceId: String,
        @RequestHeader("X-Goog-Resource-State") resourceState: String,
        @RequestHeader("X-Goog-Message-Number") messageNumber: Long,
    ) {
        channelNotificationReceiver
            .receive(GoogleChannelNotification(channelId, token, resourceId, resourceState, messageNumber))
    }
}