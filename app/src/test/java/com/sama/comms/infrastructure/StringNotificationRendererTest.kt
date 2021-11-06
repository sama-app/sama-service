package com.sama.comms.infrastructure

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.Message
import com.sama.comms.domain.NotificationData
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringNotificationRendererTest {
    private val underTest: StringNotificationRenderer = StringNotificationRenderer()

    @Test
    fun renderMeetingConfirmedSameTimeZone() {
        val timeZone = ZoneId.of("UTC")
        val commsUser = CommsUser(UserId(1), timeZone, "test@meetsama.com", "Initiator")
        val actual = underTest.renderMeetingConfirmed(
            "attendee@meetsama.com",
            commsUser,
            ZonedDateTime.of(
                LocalDate.of(2021, 6, 1),
                LocalTime.of(8, 0),
                timeZone
            )
        )

        assertThat(actual).isEqualTo(
            Message(
                notification = NotificationData(
                    title = "attendee@meetsama.com confirmed a meeting",
                    body = "Meet on Jun 1 on 8:00 AM in your time zone"
                ),
                emptyMap()
            )
        )
    }

    @Test
    fun renderMeetingConfirmedDifferentTimeZone() {
        val timeZone = ZoneId.of("UTC+10")
        val commsUser = CommsUser(UserId(1), timeZone, "test@meetsama.com", "Initiator")
        val actual = underTest.renderMeetingConfirmed(
            "attendee@meetsama.com",
            commsUser,
            ZonedDateTime.of(
                LocalDate.of(2021, 6, 1),
                LocalTime.of(8, 0),
                ZoneId.of("UTC")
            )
        )

        assertThat(actual).isEqualTo(
            Message(
                notification = NotificationData(
                    title = "attendee@meetsama.com confirmed a meeting",
                    body = "Meet on Jun 1 on 6:00 PM in your time zone"
                ),
                emptyMap()
            )
        )
    }
}