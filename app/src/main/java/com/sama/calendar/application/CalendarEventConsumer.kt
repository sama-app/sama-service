package com.sama.calendar.application

import com.sama.meeting.domain.MeetingConfirmedEvent
import org.springframework.stereotype.Component

@Component
class CalendarEventConsumer(private val eventApplicationService: EventApplicationService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val meeting = event.confirmedMeeting
        eventApplicationService.createBlock(
            meeting.initiatorId,
            CreateEventCommand(meeting.slot.startDateTime, meeting.slot.endDateTime, meeting.meetingRecipient.email!!)
        )
    }
}