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

    override fun renderMeetingConfirmed(
        commsUser: CommsUser,
        attendeeEmail: String,
        startDateTime: ZonedDateTime
    ): Notification {
        val adjustedStartDatetime = startDateTime.withZoneSameInstant(commsUser.timeZone)
        val date = dateFormatter.format(adjustedStartDatetime).removeYear()
        val time = timeFormatter.format(adjustedStartDatetime)

        return Notification(
            title = "$attendeeEmail confirmed a meeting",
            body = "Meet on $date on $time in your time zone"
        )

    }

    private fun String.removeYear(): String {
        return this.replace(Regex(", \\d{4}"), "")
    }
}