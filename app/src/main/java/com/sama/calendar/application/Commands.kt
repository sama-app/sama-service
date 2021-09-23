package com.sama.calendar.application

import com.sama.meeting.domain.MeetingRecipient
import java.time.ZonedDateTime


data class CreateEventCommand(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val recipient: MeetingRecipient,
    val title: String,
)