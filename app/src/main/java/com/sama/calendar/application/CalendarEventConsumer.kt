package com.sama.calendar.application

import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.UserRecipient
import org.springframework.stereotype.Component

@Component
class CalendarEventConsumer(private val eventApplicationService: EventApplicationService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val recipientEmail = when (val recipient = event.confirmedMeeting.meetingRecipient) {
            is EmailRecipient -> recipient.email
            is UserRecipient -> recipient.email
        }

        val meeting = event.confirmedMeeting
        eventApplicationService.createEvent(
            meeting.initiatorId,
            CreateEventCommand(
                meeting.slot.startDateTime,
                meeting.slot.endDateTime,
                recipientEmail,
                meeting.meetingTitle
            )
        )
    }
}