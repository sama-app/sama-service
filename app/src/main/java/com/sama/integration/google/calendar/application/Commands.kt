package com.sama.integration.google.calendar.application

import java.time.ZonedDateTime

data class InsertGoogleCalendarEventCommand(
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val title: String,
    val description: String?,
    val recipientEmail: String,
)