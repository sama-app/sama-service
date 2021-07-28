package com.sama.comms.application

import com.sama.users.domain.UserId
import java.time.ZonedDateTime

data class SendMeetingConfirmedCommand(
    val userId: UserId,
    val meetingAttendeeEmail: String,
    val meetingStartDateTime: ZonedDateTime,
)