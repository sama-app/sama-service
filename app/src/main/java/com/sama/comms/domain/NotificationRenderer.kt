package com.sama.comms.domain

import java.time.ZonedDateTime

interface NotificationRenderer {
    fun renderMeetingConfirmed(commsUser: CommsUser, attendeeEmail: String, startDateTime: ZonedDateTime): Notification
}