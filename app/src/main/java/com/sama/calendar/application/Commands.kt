package com.sama.calendar.application

import java.time.ZonedDateTime


data class CreateBlockCommand(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val recipientEmail: String
)