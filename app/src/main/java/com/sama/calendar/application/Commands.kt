package com.sama.calendar.application

import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingRecipient
import java.time.ZonedDateTime


data class CreateEventCommand(
    val meetingCode: MeetingCode,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val recipient: MeetingRecipient,
    val title: String,
)

data class BlockOutTimesCommand(val meetingCode: MeetingCode, val meetingTitle: String, val slots: List<Slot>)

data class Slot(val startDateTime: ZonedDateTime, val endDateTime: ZonedDateTime)