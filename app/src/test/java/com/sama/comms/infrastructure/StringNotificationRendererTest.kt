package com.sama.comms.infrastructure

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.Notification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class StringNotificationRendererTest {
    private val underTest: StringNotificationRenderer = StringNotificationRenderer()

    @Test
    fun renderMeetingConfirmedSameTimeZone() {
        val timeZone = ZoneId.of("UTC")
        val commsUser = CommsUser(1L, timeZone, "test@meetsama.com")
        val actual = underTest.renderMeetingConfirmed(
            commsUser,
            "attendee@meetsama.com",
            ZonedDateTime.of(
                LocalDate.of(2021, 6, 1),
                LocalTime.of(8, 0),
                timeZone
            )
        )

        assertThat(actual).isEqualTo(
            Notification(
                "attendee@meetsama.com confirmed a meeting",
                "Meet on Jun 1 on 8:00 AM in your time zone"
            )
        )
    }

    @Test
    fun renderMeetingConfirmedDifferentTimeZone() {
        val timeZone = ZoneId.of("UTC+10")
        val commsUser = CommsUser(1L, timeZone, "test@meetsama.com")
        val actual = underTest.renderMeetingConfirmed(
            commsUser,
            "attendee@meetsama.com",
            ZonedDateTime.of(
                LocalDate.of(2021, 6, 1),
                LocalTime.of(8, 0),
                ZoneId.of("UTC")
            )
        )

        assertThat(actual).isEqualTo(
            Notification(
                "attendee@meetsama.com confirmed a meeting",
                "Meet on Jun 1 on 6:00 PM in your time zone"
            )
        )
    }
}