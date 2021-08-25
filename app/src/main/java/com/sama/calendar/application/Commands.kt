package com.sama.calendar.application

import java.time.ZonedDateTime


data class CreateEventCommand(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val recipientEmail: String,
    val title: String,
)