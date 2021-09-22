package com.sama.comms.infrastructure

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.Notification
import com.sama.comms.domain.NotificationRenderer
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Component
class StringNotificationRenderer : NotificationRenderer {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.ENGLISH)

    override fun renderMeetingInvitation(sender: CommsUser): Notification {
        return Notification(
            title = "${sender.displayName} wants to meet",
            additionalData = emptyMap()
        )
    }

    override fun renderMeetingProposed(sender: CommsUser, receiver: CommsUser, startDateTime: ZonedDateTime): Notification {
        val adjustedStartDatetime = startDateTime.withZoneSameInstant(receiver.timeZone)
        val date = dateFormatter.format(adjustedStartDatetime).removeYear()
        val time = timeFormatter.format(adjustedStartDatetime)
        return Notification(
            title = "${sender.displayName} wants to meet on $date on $time ",
            additionalData = emptyMap()
        )
    }


    override fun renderMeetingConfirmed(senderDisplayName: String, receiver: CommsUser, startDateTime: ZonedDateTime): Notification {
        val adjustedStartDatetime = startDateTime.withZoneSameInstant(receiver.timeZone)
        val date = dateFormatter.format(adjustedStartDatetime).removeYear()
        val time = timeFormatter.format(adjustedStartDatetime)

        return Notification(
            title = "$senderDisplayName confirmed a meeting",
            body = "Meet on $date on $time in your time zone",
            additionalData = emptyMap()
        )
    }

    override fun renderConnectionRequested(sender: CommsUser): Notification {
        return Notification(
            title = "${sender.displayName} wants to connect",
            additionalData = emptyMap()
        )
    }

    override fun renderConnectionRequestRejected(sender: CommsUser): Notification {
        return Notification(
            title = "${sender.displayName} rejected your connection request",
            additionalData = emptyMap()
        )
    }

    override fun renderUserConnected(sender: CommsUser): Notification {
        return Notification(
            title = "${sender.displayName} connected with you",
            additionalData = emptyMap()
        )
    }

    private fun String.removeYear(): String {
        return this.replace(Regex(", \\d{4}"), "")
    }
}